package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class MangaDexSourceTest {
    private var now = Instant.fromEpochMilliseconds(1_790_000_000_000)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val requests = mutableListOf<HttpRequestData>()
    private val waits = mutableListOf<Duration>()
    private var route: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData? = { null }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
    ) = respond(
        body,
        status,
        headersOf(
            *(headers + (HttpHeaders.ContentType to ContentType.Application.Json.toString()))
                .map {
                    it.key to listOf(it.value)
                }.toTypedArray(),
        ),
    )

    private var report = true
    private var ratings = setOf("safe")

    private val source by lazy {
        MangaDexSource(
            http =
                HttpClient(
                    MockEngine { request ->
                        requests += request
                        route(request) ?: when {
                            request.url.encodedPath == "/manga" -> json(Fixtures.MANGA_LIST)
                            request.url.encodedPath == "/manga/tag" -> json(Fixtures.TAGS)
                            request.url.encodedPath.endsWith("/feed") -> json(Fixtures.FEED)
                            request.url.encodedPath.startsWith("/manga/") -> json(Fixtures.MANGA)
                            request.url.encodedPath.startsWith("/at-home/server/") -> json(Fixtures.AT_HOME)
                            request.url.host == "api.mangadex.network" -> respond("", HttpStatusCode.OK)
                            else -> respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK, headersOf("X-Cache", "HIT"))
                        }
                    },
                ),
            contentLanguages = { setOf("pt-br") },
            contentRatings = { ratings },
            clock = clock,
            sleep = {
                waits += it
                now += it
            },
            reportImages = report,
        )
    }

    private val manga = SourceManga(SourceId.MANGADEX, "a1", "Obra")
    private val chapter = SourceChapter("ch1", 12.0)

    @Test
    fun FEAT001_popularAsksSafeWorksInTheUserLanguageAndPages() =
        runTest {
            val page = source.popular(2)
            val url = requests.single().url
            url.parameters["order[followedCount]"] shouldBe "desc"
            url.parameters.getAll("contentRating[]") shouldBe listOf("safe")
            url.parameters.getAll("availableTranslatedLanguage[]") shouldBe listOf("pt-br")
            url.parameters["offset"] shouldBe "20"
            url.parameters.getAll("includes[]")!! shouldContain "cover_art"
            page.hasNextPage shouldBe true
            page.items.size shouldBe 2
        }

    @Test
    fun RF08_eroticaAndPornographicAreNeverAsked() =
        runTest {
            ratings = setOf("safe", "suggestive", "erotica", "pornographic")
            source.popular(1)
            requests
                .last()
                .url.parameters
                .getAll("contentRating[]")!!
                .toSet() shouldBe setOf("safe", "suggestive")
        }

    @Test
    fun RF29_workDetailsComeFromTheApi() =
        runTest {
            val work = source.details(manga)
            work.key shouldBe "a1"
            work.title shouldBe "Lâmina do Norte"
            work.alternativeTitles shouldContain "Blade of the North"
            work.authors shouldContainExactly listOf("Rui Takeda")
            work.artists shouldContainExactly listOf("Mei Lin")
            work.status shouldBe PublicationStatus.ONGOING
            work.kind shouldBe ReadingKind.MANGA
            work.genres shouldContainExactly listOf("Ação", "Fantasia")
            work.description shouldBe "Uma ferreira atravessa o gelo."
            work.coverUrl shouldBe "https://uploads.mangadex.org/covers/a1/capa.jpg.512.jpg"
            work.url shouldBe "https://mangadex.org/title/a1"
        }

    @Test
    fun RF29_emptyDescriptionArrayIsNoDescription() =
        runTest {
            route = { if (it.url.encodedPath == "/manga") json(Fixtures.MANGA_LIST) else null }
            val second = source.popular(1).items[1]
            second.description shouldBe null
            second.title shouldBe "Kaiju Express"
            // Korean without the "Long Strip" tag pages left to right (RF-37).
            second.kind shouldBe ReadingKind.COMIC
        }

    @Test
    fun RF30_chaptersWithGroupDateAndAllFeedPages() =
        runTest {
            var calls = 0
            route = {
                if (it.url.encodedPath.endsWith("/feed")) {
                    calls++
                    json(if (calls == 1) Fixtures.FEED else Fixtures.FEED_LAST)
                } else {
                    null
                }
            }
            val chapters = source.chapters(manga)
            chapters.map { it.number } shouldContainExactly listOf(12.0, 11.5, 1.0)
            chapters[0].group shouldBe "Scan Aurora"
            chapters[0].groupUrl shouldBe "https://mangadex.org/group/g1"
            chapters[0].uploadedAtEpochMs shouldBe Instant.parse("2026-10-20T12:00:00Z").toEpochMilliseconds()
            chapters[0].pageCount shouldBe 3
            chapters[1].externalUrl shouldBe "https://example.org/c"
            requests
                .filter { it.url.encodedPath.endsWith("/feed") }
                .map { it.url.parameters["offset"] } shouldContainExactly listOf("0", "2")
            requests
                .first()
                .url.parameters
                .getAll("translatedLanguage[]") shouldBe listOf("pt-br")
        }

    @Test
    fun RF09_pagesUseTheAtHomeServer() =
        runTest {
            val pages = source.pages(manga, chapter)
            pages.map { it.imageUrl } shouldContainExactly listOf("mangadex-page:ch1/p1.png", "mangadex-page:ch1/p2.png")
            source.image(pages[0].imageUrl, emptyMap()) shouldBe byteArrayOf(1, 2, 3)
            val image = requests.first { it.url.host == "node.example.net" }
            image.url.toString() shouldBe "https://node.example.net/data/hash1/p1.png"
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 1
        }

    @Test
    fun RF09_imagesOutsideMangaDexAreReported() =
        runTest {
            source.pages(manga, chapter)
            source.image("mangadex-page:ch1/p1.png", emptyMap())
            val report = requests.single { it.url.host == "api.mangadex.network" }
            val body = (report.body as TextContent).text
            body shouldContain "\"url\":\"https://node.example.net/data/hash1/p1.png\""
            body shouldContain "\"success\":true"
            body shouldContain "\"bytes\":3"
            body shouldContain "\"cached\":true"
        }

    @Test
    fun RF09_browserBuildSendsNoReport() =
        runTest {
            report = false
            source.pages(manga, chapter)
            source.image("mangadex-page:ch1/p1.png", emptyMap())
            requests.none { it.url.host == "api.mangadex.network" } shouldBe true
        }

    @Test
    fun RF09_serverIsRenewedAfter12MinutesAndOnFailure() =
        runTest {
            source.pages(manga, chapter)
            source.image("mangadex-page:ch1/p1.png", emptyMap())
            now += 13.minutes
            source.image("mangadex-page:ch1/p2.png", emptyMap())
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 2
            var failed = false
            route = {
                if (it.url.host == "node.example.net" && !failed) {
                    failed = true
                    respond("", HttpStatusCode.Forbidden)
                } else {
                    null
                }
            }
            source.image("mangadex-page:ch1/p1.png", emptyMap()) shouldBe byteArrayOf(1, 2, 3)
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 3
            requests
                .filter {
                    it.url.host == "api.mangadex.network"
                }.map { (it.body as TextContent).text }
                .any { "\"success\":false" in it } shouldBe
                true
        }

    @Test
    fun RF09_cancelledImageDoesNotRenewTheServer() =
        runTest {
            source.pages(manga, chapter)
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            route = {
                if (it.url.host == "node.example.net") {
                    gate.await()
                    null
                } else {
                    null
                }
            }
            val job = launch { source.image("mangadex-page:ch1/p1.png", emptyMap()) }
            testScheduler.advanceUntilIdle()
            job.cancel()
            job.join()
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 1
        }

    @Test
    fun RF09_parallelFailuresRenewTheServerOnce() =
        runTest {
            source.pages(manga, chapter)
            // Every image fails on the first server and works on the renewed one.
            route = {
                val atHome = requests.count { r -> r.url.encodedPath.startsWith("/at-home/") }
                if (it.url.host == "node.example.net" && atHome == 1) respond("", HttpStatusCode.Forbidden) else null
            }
            (1..5).map { n -> async { source.image("mangadex-page:ch1/p$n.png", emptyMap()) } }.awaitAll()
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 2
        }

    @Test
    fun RF04_atHomeNeverMoreThan40PerMinute() =
        runTest {
            repeat(41) { source.pages(manga, chapter.copy(key = "c$it")) }
            // The 41st at-home request waited for the minute to pass on the fake clock.
            waits.count { it > Duration.parse("30s") } shouldBe 1
            requests.count { it.url.encodedPath.startsWith("/at-home/") } shouldBe 41
        }

    @Test
    fun RF04_tooManyRequestsWithoutHeaderWaitsSixtySeconds() =
        runTest {
            var calls = 0
            route = { if (it.url.encodedPath == "/manga" && calls++ == 0) json("{}", HttpStatusCode.TooManyRequests) else null }
            source.popular(1)
            waits shouldContain Duration.parse("60s")
        }

    @Test
    fun RF04_tooManyRequestsWaitsRetryAfter() =
        runTest {
            var calls = 0
            route = {
                if (it.url.encodedPath == "/manga" &&
                    calls++ == 0
                ) {
                    json("{}", HttpStatusCode.TooManyRequests, mapOf("Retry-After" to "7"))
                } else {
                    null
                }
            }
            source.popular(1).items.size shouldBe 2
            waits shouldContain Duration.parse("7s")
        }

    @Test
    fun RF04_neverMoreThanFivePerSecond() =
        runTest {
            repeat(12) { source.details(manga) }
            // 12 calls at 5 per second: the limiter waited twice on the fake clock.
            waits.size shouldBe 2
        }

    @Test
    fun RF05_failuresKeepTheirKind() =
        runTest {
            route = { json("{}", HttpStatusCode.NotFound) }
            shouldThrow<SourceException> { source.details(manga) }.kind shouldBe SourceException.Kind.NOT_FOUND
            route = { json("<html>", HttpStatusCode.OK) }
            shouldThrow<SourceException> { source.details(manga) }.kind shouldBe SourceException.Kind.PARSE
            shouldThrow<SourceException> { source.pages(manga, chapter.copy(externalUrl = "https://x")) }.kind shouldBe
                SourceException.Kind.UNAVAILABLE
        }

    @Test
    fun RF37_firstReadSuggestionFollowsLanguageAndLongStrip() {
        mangaDexKind("ja") shouldBe ReadingKind.MANGA
        mangaDexKind("ko", longStrip = true) shouldBe ReadingKind.MANHWA
        mangaDexKind("zh", longStrip = true) shouldBe ReadingKind.MANHWA
        // A Korean work without the tag pages left to right.
        mangaDexKind("ko") shouldBe ReadingKind.COMIC
    }

    @Test
    fun RF90_keysAndMappings() =
        runTest {
            source.id shouldBe SourceId.MANGADEX
            mangaDexStatus("completed") shouldBe PublicationStatus.COMPLETED
            mangaDexStatus("hiatus") shouldBe PublicationStatus.HIATUS
            mangaDexStatus("cancelled") shouldBe PublicationStatus.CANCELLED
            mangaDexStatus(null) shouldBe PublicationStatus.UNKNOWN
            mangaDexKind("en") shouldBe ReadingKind.COMIC
            mangaDexKind(null) shouldBe ReadingKind.UNKNOWN
            source.latest(1)
            requests.last().url.parameters["order[latestUploadedChapter]"] shouldBe "desc"
            source.search(" naruto ", emptyList(), 1)
            requests.last().url.parameters["title"] shouldBe "naruto"
        }

    @Test
    fun RF22_filtersComeWithTagsFromTheApiOnce() =
        runTest {
            val filters = source.filters()
            source.filters()
            requests.count { it.url.encodedPath == "/manga/tag" } shouldBe 1
            val tags = filters.filterIsInstance<SourceFilter.Tags>().single()
            tags.options.map { it.label } shouldBe listOf("Action", "Horror", "Romance")
            filters.map { it.key } shouldBe listOf("tags", "status", "demographic", "originalLanguage", "year", "order")
        }

    @Test
    fun RF22_includedAndExcludedTagsStatusYearAndOrderGoToTheRequest() =
        runTest {
            val filters =
                source.filters().map { f ->
                    when (f) {
                        is SourceFilter.Tags -> f.copy(included = setOf("t-romance"), excluded = setOf("t-horror"))
                        is SourceFilter.Choice ->
                            when (f.key) {
                                MangaDexFilters.STATUS -> f.copy(selected = setOf("completed"))
                                MangaDexFilters.DEMOGRAPHIC -> f.copy(selected = setOf("seinen"))
                                else -> f.copy(selected = setOf("ko"))
                            }
                        is SourceFilter.Text -> f.copy(value = " 2019 ")
                        is SourceFilter.Sort -> f.copy(selected = "rating", ascending = true)
                    }
                }
            source.search("", filters, 1)
            val p = requests.last().url.parameters
            p.getAll("includedTags[]") shouldBe listOf("t-romance")
            p.getAll("excludedTags[]") shouldBe listOf("t-horror")
            p.getAll("status[]") shouldBe listOf("completed")
            p.getAll("publicationDemographic[]") shouldBe listOf("seinen")
            p.getAll("originalLanguage[]") shouldBe listOf("ko")
            p["year"] shouldBe "2019"
            p["order[rating]"] shouldBe "asc"
            p["title"] shouldBe null
            p.getAll("contentRating[]") shouldBe listOf("safe")
        }

    @Test
    fun RF22_relevanceWithoutATitleBecomesMostFollowed() =
        runTest {
            source.search("", source.filters(), 1)
            requests.last().url.parameters["order[followedCount]"] shouldBe "desc"
            source.search("frieren", source.filters(), 1)
            requests.last().url.parameters["order[relevance]"] shouldBe "desc"
        }

    @Test
    fun RF21_listEndsWhereTheApiStopsAt10000() =
        runTest {
            route = {
                if (it.url.encodedPath == "/manga") {
                    json(Fixtures.MANGA_LIST.replace("\"offset\": 20", "\"offset\": 9980").replace("\"total\": 100", "\"total\": 50000"))
                } else {
                    null
                }
            }
            source.popular(500).hasNextPage shouldBe false
        }

    @Test
    fun RF05_webPageForOpenInBrowser() {
        source.webUrl() shouldBe "https://mangadex.org/titles"
        source.webUrl("one piece") shouldBe "https://mangadex.org/search?q=one%20piece"
    }
}
