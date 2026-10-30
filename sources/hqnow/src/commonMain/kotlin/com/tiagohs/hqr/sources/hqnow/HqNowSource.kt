package com.tiagohs.hqr.sources.hqnow

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
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * HQ Now (RF-12) through its GraphQL API: most viewed, recently updated, search by name, the work with its chapters
 * and the pages. The API has no paging: "Populares" asks for a growing list and shows the new slice.
 */
class HqNowSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = SourceId.HQ_NOW
    override val name: String = "HQ Now"
    override val languages: List<String> = listOf("pt-br")

    private val json = Json { ignoreUnknownKeys = true }
    private val limiter = RateLimiter(PERMITS, 1.seconds, clock, sleep)

    override suspend fun popular(page: Int): MangaPage {
        val limit = page.coerceAtLeast(1) * PAGE_SIZE
        val all =
            query(
                "getHqsByFilters",
                "query { getHqsByFilters(orderByViews: true, limit: $limit, loadCovers: true) { $LIST_FIELDS } }",
                ListSerializer(HqDto.serializer()),
            )
        val slice = all.drop((page - 1).coerceAtLeast(0) * PAGE_SIZE)
        return MangaPage(slice.map { it.toManga() }, all.size >= limit && limit < MAX_POPULAR)
    }

    override suspend fun latest(page: Int): MangaPage {
        if (page > 1) return MangaPage(emptyList(), false)
        val list = query("getRecentlyUpdatedHqs", "query { getRecentlyUpdatedHqs { $RECENT_FIELDS } }", ListSerializer(HqDto.serializer()))
        return MangaPage(list.map { it.toManga() }, false)
    }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        if (page > 1 || query.isBlank()) return MangaPage(emptyList(), false)
        val list =
            query(
                "getHqsByName",
                "query(\$name: String!) { getHqsByName(name: \$name) { $LIST_FIELDS } }",
                ListSerializer(HqDto.serializer()),
                buildJsonObject { put("name", query.trim()) },
            )
        return MangaPage(list.map { it.toManga() }, false)
    }

    override suspend fun details(manga: SourceManga): SourceManga = work(manga).toManga()

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val work = work(manga)
        return work.capitulos
            .map { ch ->
                SourceChapter(
                    key = ch.id.toString(),
                    number = ch.number?.toDoubleOrNull() ?: 0.0,
                    title = ch.name?.takeIf { it.isNotBlank() },
                    language = "pt-br",
                )
            }.sortedByDescending { it.number }
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val id = chapter.key.toIntOrNull() ?: throw SourceException("Invalid chapter key ${chapter.key}", SourceException.Kind.PARSE)
        val result =
            query(
                "getChapterById",
                "query(\$id: Int!) { getChapterById(chapterId: \$id) { pictures { pictureUrl } } }",
                ChapterPagesDto.serializer(),
                buildJsonObject { put("id", id) },
            )
        return result.pictures.mapIndexed { i, p -> SourcePage(i, secure(p.pictureUrl), mapOf("Referer" to "$SITE/")) }
    }

    private suspend fun work(manga: SourceManga): HqDto {
        val id = manga.key.toIntOrNull() ?: throw SourceException("Invalid work key ${manga.key}", SourceException.Kind.PARSE)
        val list =
            query(
                "getHqsById",
                "query(\$id: Int!) { getHqsById(id: \$id) { $LIST_FIELDS publisherName capitulos { id name number } } }",
                ListSerializer(HqDto.serializer()),
                buildJsonObject { put("id", id) },
            )
        return list.firstOrNull() ?: throw SourceException("Work ${manga.key} not found", SourceException.Kind.NOT_FOUND)
    }

    /** One GraphQL request: limiter, status, `errors`, then `data.<field>`. */
    private suspend fun <T> query(
        field: String,
        text: String,
        serializer: KSerializer<T>,
        variables: JsonObject? = null,
    ): T {
        limiter.acquire()
        val body =
            networkCall {
                http
                    .post(API) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            json.encodeToString(
                                JsonObject.serializer(),
                                buildJsonObject {
                                    put("query", text)
                                    variables?.let { put("variables", it) }
                                },
                            ),
                        )
                    }.ensureOk()
                    .bodyAsText()
            }
        try {
            val root = json.parseToJsonElement(body).jsonObject
            if (root["errors"] != null && root["data"] == null) {
                throw SourceException("HQ Now answered with errors", SourceException.Kind.SERVER)
            }
            val value: JsonElement =
                root["data"]?.jsonObject?.get(field) ?: throw SourceException("HQ Now: no $field", SourceException.Kind.PARSE)
            return json.decodeFromJsonElement(serializer, value)
        } catch (e: IllegalArgumentException) {
            throw SourceException("HQ Now answer out of the expected format", SourceException.Kind.PARSE, e)
        }
    }

    private fun HqDto.toManga() =
        SourceManga(
            sourceId = SourceId.HQ_NOW,
            key = id.toString(),
            title = name,
            coverUrl = hqCover?.let(::secure),
            url = "$SITE/hq/$id/${slug(name)}",
            authors = emptyList(),
            status = hqNowStatus(status),
            genres = listOfNotNull(publisherName),
            description = synopsis?.trim()?.takeIf { it.isNotEmpty() },
            language = "pt-br",
            kind = ReadingKind.COMIC,
        )

    companion object {
        private const val API = "https://admin.hq-now.com/graphql"
        private const val SITE = "https://www.hq-now.com"
        private const val PAGE_SIZE = 30
        private const val MAX_POPULAR = 600
        private const val PERMITS = 3
        private const val LIST_FIELDS = "id name editoraId status hqCover synopsis updatedAt"

        // The type of "recentes" (UpdatedHqs) has no status nor publisher.
        private const val RECENT_FIELDS = "id name hqCover synopsis updatedAt"
    }
}

@Serializable
internal data class HqDto(
    val id: Int,
    val name: String,
    val status: String? = null,
    val hqCover: String? = null,
    val synopsis: String? = null,
    val publisherName: String? = null,
    val capitulos: List<ChapterDto> = emptyList(),
)

@Serializable
internal data class ChapterDto(
    val id: Int,
    val name: String? = null,
    val number: String? = null,
)

@Serializable
internal data class ChapterPagesDto(
    val pictures: List<PictureDto> = emptyList(),
)

@Serializable
internal data class PictureDto(
    val pictureUrl: String,
)

internal fun hqNowStatus(value: String?): PublicationStatus =
    when (value?.lowercase()) {
        "concluído", "concluido", "finalizado" -> PublicationStatus.COMPLETED
        "em andamento", "ativo", "lançando" -> PublicationStatus.ONGOING
        "cancelado" -> PublicationStatus.CANCELLED
        "hiato", "pausado" -> PublicationStatus.HIATUS
        else -> PublicationStatus.UNKNOWN
    }

/** The images come as http; the same host answers https, which Android requires (RNF security). */
internal fun secure(url: String): String = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url

/** Slug used by the site's addresses: "Invencível (2003)" → "invencivel-2003". */
internal fun slug(name: String): String {
    val plain =
        name
            .lowercase()
            .map { c -> ACCENTS[c] ?: c }
            .joinToString("")
    return plain.replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

private val ACCENTS =
    mapOf(
        'á' to 'a',
        'à' to 'a',
        'â' to 'a',
        'ã' to 'a',
        'ä' to 'a',
        'é' to 'e',
        'ê' to 'e',
        'è' to 'e',
        'í' to 'i',
        'ì' to 'i',
        'ó' to 'o',
        'ô' to 'o',
        'õ' to 'o',
        'ò' to 'o',
        'ú' to 'u',
        'ü' to 'u',
        'ù' to 'u',
        'ç' to 'c',
        'ñ' to 'n',
    )
