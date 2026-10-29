package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
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
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
) : CatalogSource,
    SearchSource,
    ImageSource {
    override val id: SourceId = SourceId.MANGADEX
    override val name: String = "MangaDex"
    override val languages: List<String> = listOf("pt-br", "en", "es", "es-la")

    private val json = Json { ignoreUnknownKeys = true }

    // RF-04: 5 requests per second on the API, 40 per minute on at-home.
    private val apiLimiter = RateLimiter(API_PERMITS, 1.seconds, clock, sleep)
    private val atHomeLimiter = RateLimiter(AT_HOME_PERMITS, 1.minutes, clock, sleep)

    private class Server(
        val baseUrl: String,
        val hash: String,
        val fetchedAt: Instant,
    )

    private val servers = HashMap<String, Server>()
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
            parameter("order[relevance]", "desc")
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
                contentRatings().forEach { parameter("contentRating[]", it) }
                contentLanguages().forEach { parameter("availableTranslatedLanguage[]", it) }
                order()
            }
        return MangaPage(response.data.map { it.toManga() }, response.offset + response.data.size < response.total)
    }

    override suspend fun details(manga: SourceManga): SourceManga {
        val response: EntityResponse<MangaDto> = api("$API/manga/${manga.key}") { INCLUDES.forEach { parameter("includes[]", it) } }
        return response.data.toManga()
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
                    contentRatings().forEach { parameter("contentRating[]", it) }
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
            throw SourceException("Capítulo hospedado fora do MangaDex: ${chapter.externalUrl}", SourceException.Kind.UNAVAILABLE)
        }
        val atHome = atHome(chapter.key)
        return atHome.chapter.data.mapIndexed { i, file -> SourcePage(i, "$SCHEME${chapter.key}/$file") }
    }

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        if (!url.startsWith(SCHEME)) return networkCall { http.get(url).ensureOk().bodyAsBytes() }
        val chapterId = url.removePrefix(SCHEME).substringBefore('/')
        val file = url.substringAfterLast('/')
        val first = server(chapterId, renew = false)
        return runCatching { download(first, file) }.getOrElse {
            // A 403 or any failure: ask for a new server once (RF-09).
            download(server(chapterId, renew = true), file)
        }
    }

    private suspend fun download(
        server: Server,
        file: String,
    ): ByteArray {
        val imageUrl = "${server.baseUrl}/data/${server.hash}/$file"
        val start = clock.now()
        val result =
            runCatching {
                networkCall {
                    val response = http.get(imageUrl)
                    val bytes = response.ensureOk().bodyAsBytes()
                    bytes to (response.headers["X-Cache"]?.startsWith("HIT") == true)
                }
            }
        if (!server.baseUrl.contains("mangadex.org")) {
            val (bytes, cached) = result.getOrNull() ?: (ByteArray(0) to false)
            report(ReportDto(imageUrl, result.isSuccess, bytes.size, (clock.now() - start).inWholeMilliseconds, cached))
        }
        return result.getOrThrow().first
    }

    /** The report never breaks reading. */
    private suspend fun report(report: ReportDto) {
        runCatching {
            http.post(REPORT) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ReportDto.serializer(), report))
            }
        }
    }

    private suspend fun server(
        chapterId: String,
        renew: Boolean,
    ): Server =
        serverLock.withLock {
            val known = servers[chapterId]
            if (!renew && known != null && clock.now() - known.fetchedAt < SERVER_LIFETIME) return@withLock known
            val fresh = atHomeUnlocked(chapterId)
            Server(fresh.baseUrl, fresh.chapter.hash, clock.now()).also { servers[chapterId] = it }
        }

    private suspend fun atHome(chapterId: String): AtHomeResponse =
        serverLock.withLock {
            atHomeUnlocked(chapterId).also { servers[chapterId] = Server(it.baseUrl, it.chapter.hash, clock.now()) }
        }

    private suspend fun atHomeUnlocked(chapterId: String): AtHomeResponse {
        atHomeLimiter.acquire()
        return api("$API/at-home/server/$chapterId") {}
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
                    val wait = response.headers["Retry-After"]?.toLongOrNull() ?: 1L
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

    private fun MangaDto.toManga(): SourceManga {
        val preferred = contentLanguages().toList() + listOf("en", "ja-ro", "ja")
        val titles = listOf(attributes.title) + attributes.altTitles
        val title =
            preferred.firstNotNullOfOrNull { lang -> titles.firstNotNullOfOrNull { it[lang] } } ?: attributes.title.values.firstOrNull()
                ?: id
        val cover = relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
        val descriptions = (attributes.description as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
        return SourceManga(
            sourceId = SourceId.MANGADEX,
            key = id,
            title = title,
            coverUrl = cover?.let { "$COVERS/$id/$it.512.jpg" },
            url = "$SITE/title/$id",
            alternativeTitles = titles.flatMap { it.values }.distinct().filter { it != title },
            authors = relationships.filter { it.type == "author" }.mapNotNull { it.attributes?.name }.distinct(),
            artists = relationships.filter { it.type == "artist" }.mapNotNull { it.attributes?.name }.distinct(),
            status = status(attributes.status),
            genres = attributes.tags.mapNotNull { tag -> preferred.firstNotNullOfOrNull { tag.attributes.name[it] } },
            description = preferred.firstNotNullOfOrNull { descriptions[it] }?.trim(),
            language = attributes.originalLanguage,
            kind = kind(attributes.originalLanguage),
        )
    }

    private fun ChapterDto.toChapter(): SourceChapter =
        SourceChapter(
            key = id,
            number = attributes.chapter?.toDoubleOrNull() ?: 0.0,
            title = attributes.title?.takeIf { it.isNotBlank() },
            volume = attributes.volume,
            language = attributes.translatedLanguage,
            group = relationships.firstOrNull { it.type == "scanlation_group" }?.attributes?.name,
            groupUrl = relationships.firstOrNull { it.type == "scanlation_group" }?.let { "$SITE/group/${it.id}" },
            uploadedAtEpochMs =
                (attributes.publishAt ?: attributes.readableAt)
                    ?.let {
                        runCatching { Instant.parse(it) }.getOrNull()
                    }?.toEpochMilliseconds(),
            externalUrl = attributes.externalUrl,
            pageCount = attributes.pages.takeIf { it > 0 },
        )

    companion object {
        const val SCHEME = "mangadex-page:"
        private const val API = "https://api.mangadex.org"
        private const val SITE = "https://mangadex.org"
        private const val COVERS = "https://uploads.mangadex.org/covers"
        private const val REPORT = "https://api.mangadex.network/report"
        private const val PAGE_SIZE = 20
        private const val FEED_SIZE = 500
        private const val API_PERMITS = 5
        private const val AT_HOME_PERMITS = 40
        private const val MAX_RATE_RETRIES = 3
        private const val MAX_RETRY_AFTER_S = 60L
        private val SERVER_LIFETIME = 12.minutes
        private val INCLUDES = listOf("cover_art", "author", "artist")

        internal fun status(value: String?): PublicationStatus =
            when (value) {
                "ongoing" -> PublicationStatus.ONGOING
                "completed" -> PublicationStatus.COMPLETED
                "hiatus" -> PublicationStatus.HIATUS
                "cancelled" -> PublicationStatus.CANCELLED
                else -> PublicationStatus.UNKNOWN
            }

        internal fun kind(originalLanguage: String?): ReadingKind =
            when (originalLanguage) {
                "ja" -> ReadingKind.MANGA
                "ko", "zh", "zh-hk" -> ReadingKind.MANHWA
                null -> ReadingKind.UNKNOWN
                else -> ReadingKind.COMIC
            }
    }
}
