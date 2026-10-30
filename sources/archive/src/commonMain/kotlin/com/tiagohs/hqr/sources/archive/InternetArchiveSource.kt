package com.tiagohs.hqr.sources.archive

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
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Internet Archive (RF-99): comics of the "comics" collection marked public domain (and old enough to be), found by the
 * official search API. Each item is one work with one chapter; its pages come from the IIIF manifest as JPEG.
 */
class InternetArchiveSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "Internet Archive"
    override val languages: List<String> = listOf("en")

    override fun webUrl(query: String): String = if (query.isBlank()) "$SITE/details/comics" else "$SITE/search?query=$query"

    private val json = Json { ignoreUnknownKeys = true }

    // No published limit: two requests a second, kind to a non-profit's servers.
    private val limiter = RateLimiter(2, 1.seconds, clock, sleep)

    private suspend fun <T> get(
        url: String,
        deserializer: DeserializationStrategy<T>,
        block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        limiter.acquire()
        val text = networkCall { http.get(url, block).ensureOk().bodyAsText() }
        return json.parseSource(deserializer, text)
    }

    private suspend fun find(
        page: Int,
        sort: String,
        query: String = "",
    ): MangaPage {
        // Words only: no field, wildcard or operator of the search syntax gets through.
        val terms = query.replace(UNSAFE, " ").replace(OPERATORS) { it.value.lowercase() }.trim()
        val q = if (terms.isEmpty()) PUBLIC_DOMAIN else "$PUBLIC_DOMAIN AND ($terms)"
        val answer =
            get("$SITE/advancedsearch.php", SearchDto.serializer()) {
                parameter("q", q)
                FIELDS.forEach { parameter("fl[]", it) }
                parameter("sort[]", sort)
                parameter("rows", ROWS)
                parameter("page", page.coerceAtLeast(1))
                parameter("output", "json")
            }.response
        val items = answer.docs.map { it.toManga() }
        return MangaPage(items, answer.start + answer.docs.size < answer.numFound)
    }

    override suspend fun popular(page: Int): MangaPage = find(page, "downloads desc")

    override suspend fun latest(page: Int): MangaPage = find(page, "publicdate desc")

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage = find(page, "downloads desc", query)

    override suspend fun details(manga: SourceManga): SourceManga {
        val item = get("$SITE/metadata/${manga.key}", MetadataDto.serializer())
        val m = item.metadata ?: throw SourceException("Item não encontrado", SourceException.Kind.NOT_FOUND)
        return manga.copy(
            title = m.title.text() ?: manga.title,
            coverUrl = cover(manga.key),
            url = "$SITE/details/${manga.key}",
            authors = m.creator.texts(),
            description = m.description.text()?.stripTags(),
            genres = m.subject.texts(),
            status = PublicationStatus.COMPLETED,
            kind = ReadingKind.COMIC,
            language = m.language.text(),
        )
    }

    /** One item, one chapter: the whole comic book. */
    override suspend fun chapters(manga: SourceManga): List<SourceChapter> =
        listOf(SourceChapter(key = manga.key, number = 1.0, title = manga.title, language = "en"))

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val manifest = get("$IIIF/iiif/3/${chapter.key}/manifest.json", ManifestDto.serializer())
        val pages =
            manifest.items.mapNotNull { canvas ->
                canvas.items
                    .firstOrNull()
                    ?.items
                    ?.firstOrNull()
                    ?.body
                    ?.id
            }
        if (pages.isEmpty()) throw SourceException("Item sem páginas de imagem", SourceException.Kind.NOT_FOUND)
        return pages.mapIndexed { i, url -> SourcePage(i, url) }
    }

    private fun DocDto.toManga() =
        SourceManga(
            sourceId = ID,
            key = identifier,
            title = title.text() ?: identifier,
            coverUrl = cover(identifier),
            url = "$SITE/details/$identifier",
            authors = creator.texts(),
            description = description.text()?.stripTags(),
            genres = subject.texts(),
            status = PublicationStatus.COMPLETED,
            kind = ReadingKind.COMIC,
            updatedAtEpochMs = isoEpochMs(publicdate),
        )

    private fun cover(identifier: String) = "$SITE/services/img/$identifier"

    companion object {
        val ID = SourceId("archive")
        const val SITE = "https://archive.org"
        const val IIIF = "https://iiif.archive.org"
        private const val ROWS = 30

        /**
         * The uploader declares the license, and some do it wrong; an item also has to be from before 1964, when
         * American comics not renewed fell into the public domain.
         */
        const val PUBLIC_DOMAIN =
            "collection:comics AND mediatype:texts AND licenseurl:*publicdomain* AND year:[1800 TO 1963]"
        private val FIELDS = listOf("identifier", "title", "creator", "description", "subject", "publicdate", "year")
        private val UNSAFE = Regex("[^\\p{L}\\p{N} '-]")
        private val TAG = Regex("<[^>]+>")
        private val OPERATORS = Regex("\\b(AND|OR|NOT|TO)\\b")

        private fun String.stripTags() = replace(TAG, " ").replace(Regex("\\s+"), " ").trim()
    }
}

/** A field the archive sends as text or as a list of texts. */
private fun JsonElement?.texts(): List<String> =
    when (this) {
        is JsonPrimitive -> listOfNotNull(contentOrNull?.takeIf { it.isNotBlank() })
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
        else -> emptyList()
    }

private fun JsonElement?.text(): String? = texts().takeIf { it.isNotEmpty() }?.joinToString("\n")

@Serializable
internal class SearchDto(
    val response: ResponseDto,
)

@Serializable
internal class ResponseDto(
    val numFound: Int = 0,
    val start: Int = 0,
    val docs: List<DocDto> = emptyList(),
)

@Serializable
internal class DocDto(
    val identifier: String,
    val title: JsonElement? = null,
    val creator: JsonElement? = null,
    val description: JsonElement? = null,
    val subject: JsonElement? = null,
    val publicdate: String? = null,
)

@Serializable
internal class MetadataDto(
    val metadata: ItemDto? = null,
)

@Serializable
internal class ItemDto(
    val title: JsonElement? = null,
    val creator: JsonElement? = null,
    val description: JsonElement? = null,
    val subject: JsonElement? = null,
    val language: JsonElement? = null,
)

@Serializable
internal class ManifestDto(
    val items: List<CanvasDto> = emptyList(),
)

@Serializable
internal class CanvasDto(
    val items: List<AnnotationPageDto> = emptyList(),
)

@Serializable
internal class AnnotationPageDto(
    val items: List<AnnotationDto> = emptyList(),
)

@Serializable
internal class AnnotationDto(
    val body: BodyDto? = null,
)

@Serializable
internal class BodyDto(
    val id: String,
)
