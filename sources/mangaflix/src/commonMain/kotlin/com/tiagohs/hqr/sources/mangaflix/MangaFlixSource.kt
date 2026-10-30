package com.tiagohs.hqr.sources.mangaflix

import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.data.net.parseSource
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.model.isoEpochMs
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * MangaFlix (RF-101): Brazilian manga from the site's JSON API, at most one request per second. "Populares", "Recentes"
 * and the search come whole, without pages; a work and all its chapters come in one answer.
 */
class MangaFlixSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
    private val apiUrl: () -> String = { API },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "MangaFlix"
    override val languages: List<String> = listOf("pt-br")

    override fun webUrl(query: String): String = SITE

    private val json =
        Json {
            ignoreUnknownKeys = true
            // A null where a default exists (a poster, a list) is the default, not a failure.
            coerceInputValues = true
        }
    private val limiter = RateLimiter(1, 1.seconds, clock, sleep)

    private suspend fun <T> get(
        path: String,
        deserializer: DeserializationStrategy<T>,
        vararg params: Pair<String, String>,
    ): T {
        limiter.acquire()
        val text =
            networkCall {
                http
                    .get("${apiUrl()}$path") {
                        params.forEach { (k, v) -> parameter(k, v) }
                    }.ensureOk()
                    .bodyAsText()
            }
        return json.parseSource(deserializer, text)
    }

    override suspend fun popular(page: Int): MangaPage {
        if (page > 1) return MangaPage(emptyList(), false)
        val sections = get("/browse", Envelope.serializer(kotlinx.serialization.builtins.ListSerializer(SectionDto.serializer()))).data
        return MangaPage(
            sections
                .firstOrNull { it.key == MOST_READ }
                ?.items
                .orEmpty()
                .mapNotNull { it.toManga() },
            false,
        )
    }

    override suspend fun latest(page: Int): MangaPage {
        if (page > 1) return MangaPage(emptyList(), false)
        val items = get("/latest-releases", Envelope.serializer(kotlinx.serialization.builtins.ListSerializer(MangaDto.serializer())), LANG)
        return MangaPage(items.data.mapNotNull { it.toManga() }, false)
    }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        if (page > 1 || query.isBlank()) return MangaPage(emptyList(), false)
        val found = get("/search/mangas", Envelope.serializer(WorksDto.serializer()), "query" to query.trim(), LANG)
        return MangaPage(found.data.works.mapNotNull { it.toManga() }, false)
    }

    private suspend fun work(manga: SourceManga) = get("/mangas/${manga.key}", Envelope.serializer(MangaDto.serializer())).data

    override suspend fun details(manga: SourceManga): SourceManga = work(manga).toManga() ?: manga

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> =
        work(manga).chapters.mapNotNull { c ->
            val number = c.number?.toDoubleOrNull() ?: return@mapNotNull null
            SourceChapter(
                key = c.id,
                number = number,
                title = c.name?.takeIf { it.isNotBlank() },
                language = "pt-br",
                group = c.owners.firstNotNullOfOrNull { it.ownerName() },
                uploadedAtEpochMs = isoEpochMs(c.isoDate),
                pageCount = c.pages,
            )
        }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> =
        get("/chapters/${chapter.key}", Envelope.serializer(ChapterImagesDto.serializer()), LANG)
            .data.images
            .sortedBy { it.order }
            .mapIndexed { i, image -> SourcePage(i, image.url, mapOf(REFERER to "$SITE/")) }

    private fun MangaDto.toManga(): SourceManga? =
        SourceManga(
            sourceId = ID,
            key = id,
            title = name?.takeIf { it.isNotBlank() } ?: return null,
            coverUrl = poster?.url,
            url = "$SITE/br/manga/$id",
            description = description?.takeIf { it.isNotBlank() },
            // In the search the genres are only ids; elsewhere they carry the name.
            genres = genres.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull },
            kind =
                when (contentType) {
                    "manhwa", "webtoon" -> ReadingKind.MANHWA
                    "manga" -> ReadingKind.MANGA
                    else -> ReadingKind.UNKNOWN
                },
            language = "pt-br",
        )

    companion object {
        val ID = SourceId("mangaflix")
        const val API = "https://api.mangaflix.net/v1"
        const val SITE = "https://mangaflix.net"
        private const val MOST_READ = "most-read"
        private const val REFERER = "Referer"
        private val LANG = "selected_language" to "pt-br"
    }
}

@Serializable
internal class Envelope<T>(
    val data: T,
)

@Serializable
internal class SectionDto(
    val key: String,
    val items: List<MangaDto> = emptyList(),
)

@Serializable
internal class WorksDto(
    val works: List<MangaDto> = emptyList(),
)

@Serializable
internal class PosterDto(
    @SerialName("default_url") val url: String? = null,
)

@Serializable
internal class MangaDto(
    @SerialName("_id") val id: String,
    // A work without a name shows up in the real listings (a "samurai" shelf of /browse): it is left out, not a failure.
    val name: String? = null,
    val description: String? = null,
    val poster: PosterDto? = null,
    val genres: List<JsonElement> = emptyList(),
    @SerialName("content_type") val contentType: String? = null,
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
internal class ChapterDto(
    @SerialName("_id") val id: String,
    val number: String? = null,
    val name: String? = null,
    @SerialName("iso_date") val isoDate: String? = null,
    val owners: List<JsonElement> = emptyList(),
    @SerialName("number_of_pages") val pages: Int? = null,
)

/** Owners come as objects in the work and as bare ids elsewhere: only a name is useful. */
private fun JsonElement.ownerName(): String? = ((this as? JsonObject)?.get("name") as? JsonPrimitive)?.contentOrNull

@Serializable
internal class ChapterImagesDto(
    val images: List<ImageDto> = emptyList(),
)

@Serializable
internal class ImageDto(
    @SerialName("default_url") val url: String,
    val order: Int = 0,
)
