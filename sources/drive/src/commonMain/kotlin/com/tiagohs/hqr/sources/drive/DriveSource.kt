package com.tiagohs.hqr.sources.drive

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
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Clock

/**
 * "Meu Drive" (RF-130): the works the user keeps in `Documentos/hqr/mangas` and `Documentos/hqr/hqs` on Google Drive.
 * The personal Worker reads the folder with a service account and answers `/api/v1/drive/…` to the signed-in app; the
 * source only speaks to it. A work is a folder (poster, `info.json`, a subfolder per chapter with its page images); the
 * folder it sits in says if it is a mangá or an HQ.
 */
class DriveSource(
    private val http: HttpClient,
    /** The Worker's address, without a trailing slash; "" on the web, where the app and the Worker share the origin. */
    private val hubUrl: () -> String,
    /** The account's session; null signed out. */
    private val session: suspend () -> String?,
    private val clock: Clock = Clock.System,
    /** RF-131: turns a chapter that is a PDF into page images; null where the platform can't. */
    private val pdf: PdfRenderer? = null,
) : CatalogSource,
    SearchSource,
    ImageSource {
    override val id: SourceId = ID
    override val name: String = "Meu Drive"
    override val languages: List<String> = emptyList()

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    private val lock = Mutex()
    private var cached: Pair<Long, List<WorkDto>>? = null

    private suspend fun getText(path: String): String {
        val token = session() ?: throw SourceException("Entre na conta para abrir o seu Drive", SourceException.Kind.AUTH)
        return networkCall {
            http
                .get("${hubUrl()}$API$path") { header(HttpHeaders.Authorization, "Bearer $token") }
                .ensureOk()
                .bodyAsText()
        }
    }

    // The list is one call for the whole library: kept for a minute, so browsing and searching don't repeat it.
    private suspend fun works(): List<WorkDto> =
        lock.withLock {
            val now = clock.now().toEpochMilliseconds()
            cached?.takeIf { now - it.first < CACHE_MS }?.second
                ?: json.parseSource(WorksDto.serializer(), getText("/works")).works.also { cached = now to it }
        }

    private fun WorkDto.toManga() =
        SourceManga(
            sourceId = ID,
            key = id,
            title = name,
            coverUrl = posterId?.let(::fileUrl),
            kind = if (shelf == "mangas") ReadingKind.MANGA else ReadingKind.COMIC,
        )

    override suspend fun popular(page: Int): MangaPage =
        if (page > 1) MangaPage(emptyList(), false) else MangaPage(works().map { it.toManga() }, false)

    override suspend fun latest(page: Int): MangaPage =
        if (page > 1) {
            MangaPage(emptyList(), false)
        } else {
            MangaPage(works().sortedByDescending { isoEpochMs(it.modifiedTime) ?: 0L }.map { it.toManga() }, false)
        }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        if (page > 1) return MangaPage(emptyList(), false)
        val needle = query.trim().lowercase()
        return MangaPage(works().filter { needle in it.name.lowercase() }.map { it.toManga() }, false)
    }

    override suspend fun details(manga: SourceManga): SourceManga {
        val info = detailsOf(manga.key).info
        return manga.copy(
            title = info.title?.takeIf { it.isNotBlank() } ?: manga.title,
            alternativeTitles = info.alternativeTitles,
            authors = info.authors,
            artists = info.artists,
            status = info.publicationStatus(),
            genres = info.genres,
            description = info.description,
            language = info.language,
        )
    }

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val details = detailsOf(manga.key)
        return details.chapters.mapIndexed { index, folder ->
            SourceChapter(
                key = if (folder.kind == "pdf") "$PDF_KEY${folder.id}" else folder.id,
                number = chapterNumber(folder.name) ?: (index + 1).toDouble(),
                title = details.info.chapters[folder.name]?.takeIf { it.isNotBlank() },
                language = details.info.language,
                uploadedAtEpochMs = isoEpochMs(folder.modifiedTime),
            )
        }
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        if (chapter.key.startsWith(PDF_KEY)) return pdfPages(chapter.key.removePrefix(PDF_KEY))
        val dto = json.parseSource(PagesDto.serializer(), getText("/chapters/${chapter.key}"))
        // A folder holding just a PDF is that PDF.
        if (dto.pages.isEmpty() && dto.pdf != null) return pdfPages(dto.pdf)
        return dto.pages.mapIndexed { index, page -> SourcePage(index, fileUrl(page.id)) }
    }

    private suspend fun pdfPages(fileId: String): List<SourcePage> {
        val renderer = pdf ?: throw SourceException("PDF não abre aqui", SourceException.Kind.UNAVAILABLE)
        val token = session() ?: throw SourceException("Entre na conta para abrir o seu Drive", SourceException.Kind.AUTH)
        val count = networkCall { renderer.pageCount(filesUrl(fileId), token) }
        return (0 until count).map { SourcePage(it, "$PDF_SCHEME$fileId#$it") }
    }

    private fun filesUrl(fileId: String) = "${hubUrl()}$API/files/$fileId"

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        val token = session() ?: throw SourceException("Entre na conta para abrir o seu Drive", SourceException.Kind.AUTH)
        if (url.startsWith(PDF_SCHEME)) {
            val (fileId, page) = url.removePrefix(PDF_SCHEME).split('#').let { it[0] to it.getOrElse(1) { "0" }.toInt() }
            val renderer = pdf ?: throw SourceException("PDF não abre aqui", SourceException.Kind.UNAVAILABLE)
            return networkCall { renderer.render(filesUrl(fileId), token, page) }
        }
        val fileId = url.removePrefix(SCHEME)
        return networkCall {
            http
                .get("${hubUrl()}$API/files/$fileId") { header(HttpHeaders.Authorization, "Bearer $token") }
                .ensureOk()
                .bodyAsBytes()
        }
    }

    private class Details(
        val info: DriveInfo,
        val chapters: List<FolderDto>,
    )

    private suspend fun detailsOf(workId: String): Details {
        val dto = json.parseSource(DetailsDto.serializer(), getText("/works/$workId"))
        return Details(dto.info?.let(::infoOf) ?: DriveInfo(), dto.chapters)
    }

    private fun infoOf(element: JsonElement): DriveInfo =
        runCatching { json.decodeFromJsonElement(DriveInfo.serializer(), element) }.getOrNull() ?: DriveInfo()

    @Serializable
    private data class WorksDto(
        val works: List<WorkDto> = emptyList(),
    )

    @Serializable
    private data class WorkDto(
        val id: String,
        val shelf: String,
        val name: String,
        val posterId: String? = null,
        val modifiedTime: String? = null,
    )

    @Serializable
    private data class FolderDto(
        val id: String,
        val name: String,
        val modifiedTime: String? = null,
        /** "folder" (pages inside) or "pdf" (the chapter is the file). */
        val kind: String = "folder",
    )

    @Serializable
    private data class DetailsDto(
        val info: JsonElement? = null,
        val chapters: List<FolderDto> = emptyList(),
    )

    @Serializable
    private data class PagesDto(
        val pages: List<PageDto> = emptyList(),
        val pdf: String? = null,
    )

    @Serializable
    private data class PageDto(
        val id: String,
        val name: String = "",
    )

    companion object {
        val ID = SourceId("drive")
        const val SCHEME = "drive://file/"
        const val PDF_SCHEME = "drive://pdf/"
        private const val PDF_KEY = "pdf:"
        private const val API = "/api/v1/drive"
        private const val CACHE_MS = 60_000L
        private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")

        fun fileUrl(fileId: String) = "$SCHEME$fileId"

        /** "01" → 1.0, "Capítulo 12.5" → 12.5; null when the folder's name has no number. */
        fun chapterNumber(folderName: String): Double? =
            NUMBER
                .find(folderName)
                ?.value
                ?.replace(',', '.')
                ?.toDoubleOrNull()
    }
}
