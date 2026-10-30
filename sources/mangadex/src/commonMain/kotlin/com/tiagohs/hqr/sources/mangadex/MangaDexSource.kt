package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * MangaDex through the public API v5 (RF-09, RF-10, ADR-005). Listings ask only for [contentRatings] (RF-08: "safe"
 * unless the user confirmed 18+) and for works with chapters in the user's [contentLanguages]. Pages carry an HQR address
 * (`mangadex-page:<chapter>/<file>`): the image server comes from `/at-home/server/{id}` and is asked again after 12
 * minutes, on a 403 or when an image fails; images outside mangadex.org are reported to the network (RF-09).
 */
class MangaDexSource(
    private val http: HttpClient,
    private val contentLanguages: () -> Set<String> = { setOf("pt-br") },
    private val contentRatings: () -> Set<String> = { setOf("safe") },
    private val clock: Clock = Clock.System,
    private val sleep: suspend (Duration) -> Unit = { delay(it) },
    // The report endpoint answers no CORS preflight, so the browser build turns it off.
    private val reportImages: Boolean = true,
    // Where reports run without holding the page; null sends them in line (tests).
    private val reportScope: CoroutineScope? = null,
) : CatalogSource,
    SearchSource,
    ImageSource {
    override val id: SourceId = SourceId.MANGADEX
    override val name: String = "MangaDex"
    override val languages: List<String> = listOf("pt-br", "en", "es", "es-la")

    override fun webUrl(query: String): String =
        if (query.isBlank()) "https://mangadex.org/titles" else "https://mangadex.org/search?q=${query.trim().encodeURLQueryComponent()}"

    private val json = Json { ignoreUnknownKeys = true }

    // RF-04: 5 requests per second on the API, 40 per minute on at-home.
    private val apiLimiter = RateLimiter(API_PERMITS, 1.seconds, clock, sleep)
    private val atHomeLimiter = RateLimiter(AT_HOME_PERMITS, 1.minutes, clock, sleep)

    private class Server(
        val baseUrl: String,
        val hash: String,
        val files: List<String>,
        val fetchedAt: Instant,
    )

    private val servers = HashMap<String, Server>()
    private val inFlight = HashMap<String, CompletableDeferred<Server>>()
    private val serverLock = Mutex()

    override suspend fun popular(page: Int): MangaPage = list(page) { parameter("order[followedCount]", "desc") }

    override suspend fun latest(page: Int): MangaPage = list(page) { parameter("order[latestUploadedChapter]", "desc") }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage =
        list(page) {
            if (query.isNotBlank()) parameter("title", query.trim())
            applyFilters(filters, hasQuery = query.isNotBlank())
        }

    // RF-22: tags come from the API once per run; the other filters are fixed by the API.
    private var tags: List<SourceFilter.Option>? = null
    private val tagsLock = Mutex()

    override suspend fun filters(): List<SourceFilter> {
        val options =
            tagsLock.withLock {
                tags ?: api<ListResponse<TagDto>>("$API/manga/tag") {}
                    .data
                    .map {
                        SourceFilter.Option(
                            it.id,
                            it.attributes.name["en"] ?: it.attributes.name.values
                                .firstOrNull() ?: it.id,
                        )
                    }.sortedBy { it.label.lowercase() }
                    .also { tags = it }
            }
        return MangaDexFilters.defaults(options)
    }

    private suspend fun list(
        page: Int,
        order: HttpRequestBuilder.() -> Unit,
    ): MangaPage {
        val response: ListResponse<MangaDto> =
            api("$API/manga") {
                parameter("limit", PAGE_SIZE)
                parameter("offset", (page - 1).coerceAtLeast(0) * PAGE_SIZE)
                parameter("hasAvailableChapters", true)
                INCLUDES.forEach { parameter("includes[]", it) }
                allowedRatings(contentRatings()).forEach { parameter("contentRating[]", it) }
                contentLanguages().forEach { parameter("availableTranslatedLanguage[]", it) }
                order()
            }
        // The API refuses offset + limit beyond 10 000: the list ends there.
        val next = response.offset + response.data.size
        return MangaPage(response.data.map { it.toManga(preferred()) }, next < response.total && next + PAGE_SIZE <= MAX_RESULTS)
    }

    private fun preferred(): List<String> = contentLanguages().toList() + listOf("en", "ja-ro", "ja")

    override suspend fun details(manga: SourceManga): SourceManga {
        val response: EntityResponse<MangaDto> = api("$API/manga/${manga.key}") { INCLUDES.forEach { parameter("includes[]", it) } }
        return response.data.toManga(preferred())
    }

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val all = ArrayList<SourceChapter>()
        var offset = 0
        do {
            val response: ListResponse<ChapterDto> =
                api("$API/manga/${manga.key}/feed") {
                    parameter("limit", FEED_SIZE)
                    parameter("offset", offset)
                    parameter("includes[]", "scanlation_group")
                    parameter("order[volume]", "desc")
                    parameter("order[chapter]", "desc")
                    contentLanguages().forEach { parameter("translatedLanguage[]", it) }
                    allowedRatings(contentRatings()).forEach { parameter("contentRating[]", it) }
                }
            all += response.data.map { it.toChapter() }
            offset += response.data.size
        } while (response.data.isNotEmpty() && offset < response.total)
        return all
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        if (chapter.externalUrl != null) {
            throw SourceException("Chapter hosted outside MangaDex: ${chapter.externalUrl}", SourceException.Kind.UNAVAILABLE)
        }
        val server = server(chapter.key, failed = null, fresh = true)
        return server.files.mapIndexed { i, file -> SourcePage(i, "$SCHEME${chapter.key}/$file") }
    }

    @Suppress("SwallowedException") // The failure is answered with a new server, not lost.
    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        if (!url.startsWith(SCHEME)) return networkCall { http.get(url).ensureOk().bodyAsBytes() }
        val chapterId = url.removePrefix(SCHEME).substringBefore('/')
        val file = url.substringAfterLast('/')
        val first = server(chapterId, failed = null)
        return try {
            download(first, file)
        } catch (e: CancellationException) {
            // A page leaving the screen is not a server failure: no renewal, no at-home quota spent.
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // A 403 or any failure: a new server, once for all the pages that failed on this one (RF-09).
            download(server(chapterId, failed = first), file)
        }
    }

    private suspend fun download(
        server: Server,
        file: String,
    ): ByteArray {
        val imageUrl = "${server.baseUrl}/data/${server.hash}/$file"
        val start = clock.now()
        val result =
            try {
                Result.success(
                    networkCall {
                        val response = http.get(imageUrl)
                        val bytes = response.ensureOk().bodyAsBytes()
                        bytes to (response.headers["X-Cache"]?.startsWith("HIT") == true)
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Result.failure(e)
            }
        if (reportImages && !Url(server.baseUrl).host.endsWith("mangadex.org")) {
            val (bytes, cached) = result.getOrNull() ?: (ByteArray(0) to false)
            val report = ReportDto(imageUrl, result.isSuccess, bytes.size, (clock.now() - start).inWholeMilliseconds, cached)
            // The report never delays nor breaks reading.
            reportScope?.launch { report(report) } ?: report(report)
        }
        return result.getOrThrow().first
    }

    private suspend fun report(report: ReportDto) {
        try {
            http.post(REPORT) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ReportDto.serializer(), report))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            // Reports are best effort.
        }
    }

    /**
     * The at-home server of a chapter. A cached one is reused for 12 minutes unless it is [failed]; one request at a
     * time per chapter, and the wait for a 429 never holds the other chapters (RF-09, RF-04).
     */
    @Suppress("TooGenericExceptionCaught") // Any failure is handed to the pages waiting, then rethrown.
    private suspend fun server(
        chapterId: String,
        failed: Server?,
        fresh: Boolean = false,
    ): Server {
        val (pending, owner) =
            serverLock.withLock {
                val known = servers[chapterId]
                val usable = known != null && known !== failed && clock.now() - known.fetchedAt < SERVER_LIFETIME
                if (!fresh && usable) return known!!
                // Another page already renewed the server that failed for this one.
                if (failed != null && known != null && known !== failed) return known
                inFlight[chapterId]?.let { it to false } ?: CompletableDeferred<Server>().also { inFlight[chapterId] = it } to true
            }
        if (owner) {
            try {
                atHomeLimiter.acquire()
                val answer: AtHomeResponse = api("$API/at-home/server/$chapterId") {}
                val server = Server(answer.baseUrl, answer.chapter.hash, answer.chapter.data, clock.now())
                serverLock.withLock {
                    servers[chapterId] = server
                    inFlight.remove(chapterId)
                }
                pending.complete(server)
            } catch (e: Throwable) {
                serverLock.withLock { inFlight.remove(chapterId) }
                pending.completeExceptionally(e)
                throw e
            }
        }
        return pending.await()
    }

    /** One API call: limiter, 429 with Retry-After (RF-04), status to source failure, JSON. */
    private suspend inline fun <reified T> api(
        url: String,
        crossinline block: HttpRequestBuilder.() -> Unit,
    ): T {
        val text =
            networkCall {
                var response: HttpResponse
                var attempt = 0
                while (true) {
                    apiLimiter.acquire()
                    response = http.get(url) { block() }
                    if (response.status != HttpStatusCode.TooManyRequests || attempt++ >= MAX_RATE_RETRIES) break
                    val wait = response.headers["Retry-After"]?.toLongOrNull() ?: DEFAULT_RETRY_AFTER_S
                    sleep(wait.coerceIn(1, MAX_RETRY_AFTER_S).seconds)
                }
                response.ensureOk().bodyAsText()
            }
        return try {
            json.decodeFromString<T>(text)
        } catch (e: IllegalArgumentException) {
            throw SourceException("Resposta do MangaDex fora do formato esperado", SourceException.Kind.PARSE, e)
        }
    }

    companion object {
        const val SCHEME = "mangadex-page:"
        private const val API = "https://api.mangadex.org"
        private const val REPORT = "https://api.mangadex.network/report"
        private const val PAGE_SIZE = 20
        private const val MAX_RESULTS = 10_000
        private const val FEED_SIZE = 500
        private const val API_PERMITS = 5
        private const val AT_HOME_PERMITS = 40
        private const val MAX_RATE_RETRIES = 3
        private const val MAX_RETRY_AFTER_S = 120L
        private const val DEFAULT_RETRY_AFTER_S = 60L
        private val SERVER_LIFETIME = 12.minutes
        private val INCLUDES = listOf("cover_art", "author", "artist")
    }
}
