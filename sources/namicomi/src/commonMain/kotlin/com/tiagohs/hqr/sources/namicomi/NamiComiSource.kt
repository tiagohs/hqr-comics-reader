package com.tiagohs.hqr.sources.namicomi

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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * NamiComi (RF-100): independent manga and comics, with Portuguese, through its API — the same envelope and
 * relationships as MangaDex, other names for the filters and its own image server. Paid chapters are left out.
 */
@Suppress("TooManyFunctions") // The source contracts plus one mapper per answer.
class NamiComiSource(
    private val http: HttpClient,
    private val contentLanguages: () -> Set<String> = { setOf("pt-br") },
    /** RF-08: "safe", plus "restricted" after the 18+ confirmation; never "mature". */
    private val contentRatings: () -> Set<String> = { setOf("safe") },
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "NamiComi"
    override val languages: List<String> = listOf("pt-br", "en", "es-419", "fr", "it")

    override fun webUrl(query: String): String = SITE

    private val json = Json { ignoreUnknownKeys = true }
    private val limiter = RateLimiter(PER_SECOND, 1.seconds, clock, sleep)

    private suspend fun <T> call(
        deserializer: DeserializationStrategy<T>,
        request: suspend HttpClient.() -> io.ktor.client.statement.HttpResponse,
    ): T {
        limiter.acquire()
        val text = networkCall { http.request().ensureOk().bodyAsText() }
        return json.parseSource(deserializer, text)
    }

    private suspend fun <T> get(
        path: String,
        deserializer: DeserializationStrategy<T>,
        block: HttpRequestBuilder.() -> Unit = {},
    ): T = call(deserializer) { get("$API$path", block) }

    private fun HttpRequestBuilder.listing(page: Int) {
        parameter("limit", PAGE)
        parameter("offset", (page.coerceAtLeast(1) - 1) * PAGE)
        contentLanguages().forEach { parameter("availableTranslatedLanguages[]", it) }
        // Never "mature": without this the API sends every rating.
        contentRatings().filter { it != "mature" }.ifEmpty { setOf("safe") }.forEach { parameter("contentRatings[]", it) }
        parameter("hasAvailableChapters", "true")
        INCLUDES.forEach { parameter("includes[]", it) }
    }

    private suspend fun list(
        page: Int,
        block: HttpRequestBuilder.() -> Unit,
    ): MangaPage {
        val answer =
            get("/title/search", Many.serializer(TitleDto.serializer())) {
                listing(page)
                block()
            }
        val meta = answer.meta
        return MangaPage(answer.data.map { it.toManga() }, meta != null && meta.offset + meta.limit < meta.total)
    }

    override suspend fun popular(page: Int): MangaPage = list(page) { parameter("order[views]", "desc") }

    override suspend fun latest(page: Int): MangaPage = list(page) { parameter("order[publishedAt]", "desc") }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage =
        list(page) {
            if (query.isNotBlank()) parameter("title", query.trim())
            parameter("order[views]", "desc")
        }

    override suspend fun details(manga: SourceManga): SourceManga =
        get("/title/${manga.key}", Entity.serializer(TitleDto.serializer())) {
            INCLUDES.forEach { parameter("includes[]", it) }
        }.data.toManga()

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val all = mutableListOf<ChapterDto>()
        var offset = 0
        do {
            val page =
                get("/chapter", Many.serializer(ChapterDto.serializer())) {
                    parameter("titleId", manga.key)
                    contentLanguages().forEach { parameter("translatedLanguages[]", it) }
                    parameter("includes[]", "organization")
                    parameter("limit", CHAPTERS_PAGE)
                    parameter("offset", offset)
                    parameter("order[volume]", "desc")
                    parameter("order[chapter]", "desc")
                }
            all += page.data
            offset += CHAPTERS_PAGE
        } while (page.meta != null && offset < page.meta.total && page.data.isNotEmpty())
        val open = openChapters(all.map { it.id })
        return all.filter { it.id in open }.mapNotNull { it.toChapter() }
    }

    /** Paid or locked chapters answer 402 to the pages: the gate says which are open, 200 at a time. */
    private suspend fun openChapters(ids: List<String>): Set<String> =
        ids
            .chunked(GATE_CHUNK)
            .flatMap { chunk ->
                try {
                    val map =
                        call(Entity.serializer(GateDto.serializer())) {
                            post("$API/gating/check") {
                                contentType(ContentType.Application.Json)
                                setBody(
                                    buildJsonObject {
                                        putJsonArray("entities") {
                                            chunk.forEach { id ->
                                                addJsonObject {
                                                    put("entityId", id)
                                                    put("entityType", "chapter")
                                                }
                                            }
                                        }
                                    }.toString(),
                                )
                            }
                        }.data.attributes.map
                    chunk.filter { map[it] != false }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("SwallowedException") e: SourceException,
                ) {
                    // Without the gate the chapters still show; a paid one fails on opening, with its reason.
                    chunk
                }
            }.toSet()

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val images =
            get("/images/chapter/${chapter.key}", Entity.serializer(ImagesDto.serializer())) { parameter("newQualities", "true") }.data
        // The "high" quality, else the original; each lives under its own name in the address.
        val (quality, files) = if (images.high.isNotEmpty()) "high" to images.high else "source" to images.source
        return files.mapIndexed { i, file ->
            SourcePage(i, "${images.baseUrl}/chapter/${chapter.key}/${images.hash}/$quality/${file.filename}")
        }
    }

    private fun TitleDto.toManga(): SourceManga {
        val preferred = contentLanguages().toList()
        val a = attributes
        return SourceManga(
            sourceId = ID,
            key = id,
            title = a.title.pick(preferred) ?: id,
            coverUrl = cover(preferred),
            url = "$SITE/${preferred.firstOrNull() ?: "en"}/title/$id",
            authors = relationships.filter { it.type == "organization" }.mapNotNull { it.attributes?.name },
            description = a.description.pick(preferred),
            genres = relationships.filter { it.type.endsWith("tag") }.mapNotNull { it.attributes?.nameMap.pick(preferred) }.distinct(),
            status =
                when (a.publicationStatus) {
                    "ongoing" -> PublicationStatus.ONGOING
                    "completed" -> PublicationStatus.COMPLETED
                    "hiatus" -> PublicationStatus.HIATUS
                    "cancelled" -> PublicationStatus.CANCELLED
                    else -> PublicationStatus.UNKNOWN
                },
            kind =
                when (a.type) {
                    "manga" -> ReadingKind.MANGA
                    "manwha", "manhwa", "manhua" -> ReadingKind.MANHWA
                    "comic" -> ReadingKind.COMIC
                    else -> ReadingKind.UNKNOWN
                },
            language = a.originalLanguage,
            updatedAtEpochMs = isoEpochMs(a.publishedAt),
        )
    }

    /** One cover per language: the reader's, then English, then any. */
    private fun TitleDto.cover(preferred: List<String>): String? {
        val covers = relationships.filter { it.type == "cover_art" && it.attributes?.fileName != null }
        val pick =
            preferred.firstNotNullOfOrNull { lang -> covers.firstOrNull { it.attributes?.locale == lang } }
                ?: covers.firstOrNull { it.attributes?.locale == "en" }
                ?: covers.firstOrNull()
        return pick?.let { "$UPLOADS/covers/$id/${it.attributes!!.fileName}.512.jpg" }
    }

    private fun ChapterDto.toChapter(): SourceChapter? {
        val a = attributes
        val number = a.chapter?.toDoubleOrNull() ?: return null
        return SourceChapter(
            key = id,
            number = number,
            title = a.name?.takeIf { it.isNotBlank() },
            volume = a.volume,
            language = a.translatedLanguage,
            group = relationships.firstOrNull { it.type == "organization" }?.attributes?.name,
            uploadedAtEpochMs = isoEpochMs(a.publishAt),
        )
    }

    private fun Map<String, String>?.pick(preferred: List<String>): String? =
        this?.let { map -> preferred.firstNotNullOfOrNull { map[it] } ?: map["en"] ?: map.values.firstOrNull() }

    companion object {
        val ID = SourceId("namicomi")
        const val API = "https://api.namicomi.com"
        const val UPLOADS = "https://uploads.namicomi.com"
        const val SITE = "https://namicomi.com"
        private const val PAGE = 20
        private const val CHAPTERS_PAGE = 200
        private const val GATE_CHUNK = 200
        private const val PER_SECOND = 3
        private val INCLUDES = listOf("cover_art", "organization", "tag", "primary_tag", "secondary_tag")
    }
}

@Serializable
internal class Meta(
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
internal class Many<T>(
    val data: List<T> = emptyList(),
    val meta: Meta? = null,
)

@Serializable
internal class Entity<T>(
    val data: T,
)

@Serializable
internal class TitleDto(
    val id: String,
    val attributes: TitleAttributes,
    val relationships: List<RelationDto> = emptyList(),
)

@Serializable
internal class TitleAttributes(
    val title: Map<String, String> = emptyMap(),
    val description: Map<String, String> = emptyMap(),
    val originalLanguage: String? = null,
    val type: String? = null,
    val publicationStatus: String? = null,
    val publishedAt: String? = null,
)

@Serializable
internal class RelationDto(
    val id: String,
    val type: String,
    val attributes: RelationAttributes? = null,
)

@Serializable
internal class RelationAttributes(
    // An organization's name is text; a tag's is one per language.
    @kotlinx.serialization.SerialName("name") val rawName: kotlinx.serialization.json.JsonElement? = null,
    val fileName: String? = null,
    val locale: String? = null,
) {
    val name: String?
        get() = (rawName as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content

    val nameMap: Map<String, String>?
        get() =
            (rawName as? kotlinx.serialization.json.JsonObject)
                ?.mapNotNull { (k, v) ->
                    (v as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { k to it }
                }?.toMap()
}

@Serializable
internal class ChapterDto(
    val id: String,
    val attributes: ChapterAttributes,
    val relationships: List<RelationDto> = emptyList(),
)

@Serializable
internal class ChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val name: String? = null,
    val translatedLanguage: String? = null,
    val publishAt: String? = null,
)

@Serializable
internal class GateDto(
    val attributes: GateAttributes,
)

@Serializable
internal class GateAttributes(
    val map: Map<String, Boolean> = emptyMap(),
)

@Serializable
internal class ImagesDto(
    val baseUrl: String,
    val hash: String,
    val source: List<ImageFile> = emptyList(),
    val high: List<ImageFile> = emptyList(),
)

@Serializable
internal class ImageFile(
    val filename: String,
)
