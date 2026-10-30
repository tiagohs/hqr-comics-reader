package com.tiagohs.hqr.sources.yomu

import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.data.net.parseSource
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.model.isoEpochMs
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Yomu Mangás (RF-102): Brazilian manga from the site's JSON API, at most one request per second; the pages come from
 * the chapter's page on the site, where they are listed as "b2://" addresses. The work key is the id; the slug only
 * goes in the page address.
 */
class YomuSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
    private val apiUrl: () -> String = { API },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "Yomu Mangás"
    override val languages: List<String> = listOf("pt-br")

    override fun webUrl(query: String): String = SITE

    private val json = Json { ignoreUnknownKeys = true }
    private val limiter = RateLimiter(1, 1.seconds, clock, sleep)

    private suspend fun text(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): String {
        limiter.acquire()
        return networkCall {
            http
                .get(url) {
                    header("Referer", "$SITE/")
                    header("Origin", SITE)
                    block()
                }.ensureOk()
                .bodyAsText()
        }
    }

    private suspend fun browse(
        page: Int,
        query: String,
    ): MangaPage {
        val answer =
            json.parseSource(
                SearchDto.serializer(),
                text("${apiUrl()}/search/medias") {
                    parameter("page", page.coerceAtLeast(1))
                    if (query.isNotBlank()) parameter("q", query.trim())
                    parameter("nsfw", "false")
                },
            )
        return MangaPage(answer.mangas.map { it.toManga() }, page < answer.pages)
    }

    override suspend fun popular(page: Int): MangaPage = browse(page, "")

    override suspend fun latest(page: Int): MangaPage {
        val answer =
            json.parseSource(
                UpdatesDto.serializer(),
                text("${apiUrl()}/home/updates") { parameter("page", page.coerceAtLeast(1)) },
            )
        return MangaPage(answer.medias.map { it.toManga() }, answer.medias.isNotEmpty() && page < MAX_LATEST_PAGES)
    }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage = browse(page, query)

    private fun numericId(key: String) = key.substringBefore('/')

    /** The work's page on the site ("/mangas/123/solo-leveling"), where its chapters are read. */
    private fun workPath(manga: SourceManga) =
        manga.url?.substringAfter(SITE)?.takeIf { it.startsWith("/mangas/") } ?: "/mangas/${manga.key}"

    override suspend fun details(manga: SourceManga): SourceManga =
        json.parseSource(WorkDto.serializer(), text("${apiUrl()}/mangas/${numericId(manga.key)}")).manga.toManga()

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> =
        json
            .parseSource(ChaptersDto.serializer(), text("${apiUrl()}/mangas/${numericId(manga.key)}/chapters"))
            .chapters
            .mapNotNull { c ->
                val number = c.chapter.toDoubleOrNull() ?: return@mapNotNull null
                SourceChapter(
                    key = c.chapter,
                    number = number,
                    title =
                        c.title
                            ?.trim()
                            ?.removePrefix("-")
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() },
                    language = "pt-br",
                    uploadedAtEpochMs = isoEpochMs(c.uploadedAt),
                )
            }.reversed()

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val html = text("$SITE${workPath(manga)}/${chapter.key}")
        val images =
            PAGE
                .findAll(html)
                .map { it.value.replace("\\/", "/") }
                .distinct()
                .toList()
        if (images.isEmpty()) throw SourceException("Páginas não encontradas", SourceException.Kind.PARSE)
        return images.mapIndexed { i, b2 -> SourcePage(i, b2Url(b2)!!, mapOf("Referer" to "$SITE/")) }
    }

    private fun MangaDto.toManga() =
        SourceManga(
            sourceId = ID,
            // The id only: a slug the site changes would turn the work into another one.
            key = id.toString(),
            title = title,
            coverUrl = b2Url(cover),
            url = "$SITE/mangas/$id/$slug",
            authors = authors,
            artists = artists,
            description = description,
            genres = (genres + tags).map { it.name },
            status =
                when (status) {
                    "ONGOING" -> PublicationStatus.ONGOING
                    "COMPLETE" -> PublicationStatus.COMPLETED
                    "HIATUS" -> PublicationStatus.HIATUS
                    "CANCELLED" -> PublicationStatus.CANCELLED
                    else -> PublicationStatus.UNKNOWN
                },
            kind =
                when (type) {
                    "MANHWA", "MANHUA" -> ReadingKind.MANHWA
                    "MANGA" -> ReadingKind.MANGA
                    else -> ReadingKind.UNKNOWN
                },
            language = "pt-br",
        )

    companion object {
        val ID = SourceId("yomu")
        const val API = "https://api.yomumangas.com"
        const val SITE = "https://yomumangas.com"
        private const val B2 = "https://b2.yomumangas.com/"
        private const val MAX_LATEST_PAGES = 20
        private val PAGE = Regex("b2://chapters/[^\"\\\\]+")

        fun b2Url(address: String?): String? = address?.let { if (it.startsWith(SCHEME)) B2 + it.removePrefix(SCHEME) else it }

        private const val SCHEME = "b2://"
    }
}

@Serializable
internal class SearchDto(
    val mangas: List<MangaDto> = emptyList(),
    val pages: Int = 1,
)

@Serializable
internal class UpdatesDto(
    val medias: List<MangaDto> = emptyList(),
)

@Serializable
internal class WorkDto(
    val manga: MangaDto,
)

@Serializable
internal class NameDto(
    val name: String,
)

@Serializable
internal class MangaDto(
    val id: Int,
    val slug: String,
    val title: String,
    val cover: String? = null,
    val type: String? = null,
    val status: String? = null,
    val description: String? = null,
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val genres: List<NameDto> = emptyList(),
    val tags: List<NameDto> = emptyList(),
)

@Serializable
internal class ChaptersDto(
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
internal class ChapterDto(
    val chapter: String,
    val title: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
)
