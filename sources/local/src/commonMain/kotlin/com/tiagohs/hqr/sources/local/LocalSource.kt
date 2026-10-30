package com.tiagohs.hqr.sources.local

import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SourceException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The user's own folder as a source (RF-13, RF-16). Each subfolder or archive in the root is a work; inside a work
 * folder, each archive or subfolder of images is a chapter, and loose images make a single chapter. The work key is
 * the path relative to the chosen folder (RF-90). Nothing is ever written.
 */
class LocalSource(
    private val tree: FileTree,
    private val pdf: PdfPages? = null,
) : CatalogSource,
    ImageSource {
    override val id: SourceId = SourceId.LOCAL

    // Shown translated by the screens (sourceDisplayName).
    override val name: String = "Local files"
    override val languages: List<String> = emptyList()

    override suspend fun popular(page: Int): MangaPage = pageOf(works().sortedWith(compareBy(NaturalOrder) { it.title }), false)

    override suspend fun latest(page: Int): MangaPage = MangaPage(works().sortedByDescending { it.updated }.map { it.manga }, false)

    private data class Work(
        val manga: SourceManga,
        val updated: Long,
    ) {
        val title: String get() = manga.title
    }

    private suspend fun works(): List<Work> {
        if (!tree.isReady()) throw SourceException("Escolha uma pasta", SourceException.Kind.UNAVAILABLE)
        return tree
            .children("")
            .filter { it.isDirectory || LocalFormat.of(it.name) != null }
            .filterNot { it.name.startsWith(".") }
            .map { Work(manga(it), it.lastModifiedEpochMs) }
    }

    private fun pageOf(
        works: List<Work>,
        hasNext: Boolean,
    ) = MangaPage(works.map { it.manga }, hasNext)

    private fun manga(node: FileNode) =
        SourceManga(
            sourceId = id,
            key = node.path,
            title = if (node.isDirectory) node.name else node.name.substringBeforeLast('.'),
            coverUrl = coverUrl(node.path),
            kind = ReadingKind.COMIC,
        )

    override suspend fun details(manga: SourceManga): SourceManga = manga.copy(coverUrl = coverUrl(manga.key))

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val key = manga.key
        if (LocalFormat.of(key) != null) return listOf(chapter(key, 1.0, manga.title))
        val children = tree.children(key).filterNot { it.name.startsWith(".") }
        val parts =
            children
                .filter { it.isDirectory || LocalFormat.of(it.name) != null }
                .sortedWith(compareBy(NaturalOrder) { it.name })
        val loose = children.any { !it.isDirectory && LocalFormat.isImage(it.name) }
        val chapters =
            parts.mapIndexed { index, node ->
                chapter(node.path, numberFrom(node.name) ?: (index + 1).toDouble(), node.name.substringBeforeLast('.'))
            }
        return if (loose) listOf(chapter(key, 1.0, manga.title)) + chapters.map { it.copy(number = it.number + 1) } else chapters
    }

    private fun chapter(
        path: String,
        number: Double,
        title: String,
    ) = SourceChapter(key = path, number = number, title = title)

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> = pageRefs(chapter.key).mapIndexed { index, ref -> SourcePage(index, ref) }

    /** Image references of a chapter: "local:<archive>!<entry>", "local:<file>" or "local:<pdf>#<page>". */
    suspend fun pageRefs(path: String): List<String> =
        when (LocalFormat.of(path)) {
            LocalFormat.CBZ -> ZipReader(tree, path).images().map { "$SCHEME$path!${it.name}" }
            LocalFormat.PDF -> {
                val reader = pdf ?: throw SourceException("PDF não abre aqui", SourceException.Kind.UNAVAILABLE)
                (0 until reader.pageCount(path)).map { "$SCHEME$path#$it" }
            }
            LocalFormat.CBR, LocalFormat.CB7 -> throw SourceException("Formato ainda não suportado", SourceException.Kind.UNAVAILABLE)
            null ->
                tree
                    .children(path)
                    .filter { !it.isDirectory && LocalFormat.isImage(it.name) }
                    .sortedWith(compareBy(NaturalOrder) { it.name })
                    .map { "$SCHEME${it.path}" }
        }

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray = load(url)

    /** Loads the bytes of a local image reference (page or cover). */
    suspend fun load(ref: String): ByteArray {
        val target = ref.removePrefix(SCHEME)
        if (target.startsWith(COVER)) return cover(target.removePrefix(COVER))
        // The archive ends at its extension followed by "!": names may contain "!" themselves.
        val bang = ARCHIVE_ENTRY.find(target)?.let { it.range.last }
        val hash = target.lastIndexOf('#')
        return when {
            bang != null -> {
                val archive = target.substring(0, bang)
                val zip = ZipReader(tree, archive)
                val name = target.substring(bang + 1)
                val entry =
                    entries(archive, zip).firstOrNull { it.name == name }
                        ?: throw SourceException("Página não encontrada", SourceException.Kind.NOT_FOUND)
                zip.read(entry)
            }
            hash > 0 && LocalFormat.of(target.substring(0, hash)) == LocalFormat.PDF ->
                requireNotNull(pdf).render(target.substring(0, hash), target.substring(hash + 1).toInt())
            else -> tree.read(target, 0, tree.size(target).toInt())
        }
    }

    private val entriesLock = Mutex()
    private var lastEntries: Pair<String, List<ZipReader.Entry>>? = null

    /** The central directory of the archive being read, kept between its pages. */
    private suspend fun entries(
        archive: String,
        zip: ZipReader,
    ): List<ZipReader.Entry> =
        entriesLock.withLock {
            lastEntries?.takeIf { it.first == archive }?.second ?: zip.entries().also { lastEntries = archive to it }
        }

    private suspend fun cover(workPath: String): ByteArray {
        val first =
            if (LocalFormat.of(workPath) != null) {
                workPath
            } else {
                chapters(SourceManga(id, workPath, workPath)).firstOrNull()?.key
                    ?: throw SourceException("Sem capa", SourceException.Kind.NOT_FOUND)
            }
        val ref = pageRefs(first).firstOrNull() ?: throw SourceException("Sem capa", SourceException.Kind.NOT_FOUND)
        return load(ref)
    }

    companion object {
        const val SCHEME = "local:"
        private val ARCHIVE_ENTRY = Regex("\\.(cbz|zip)!", RegexOption.IGNORE_CASE)
        private const val COVER = "cover:"

        fun coverUrl(workPath: String) = "$SCHEME$COVER$workPath"

        private val number = Regex("(?i)(?:cap(?:[íi]tulo)?|ch(?:apter)?|ed(?:i[çc][ãa]o)?|vol(?:ume)?|#|\\b)\\.?\\s*(\\d+(?:\\.\\d+)?)")

        fun numberFrom(name: String): Double? {
            val base = if (name.substringAfterLast('.', "").any { it.isLetter() }) name.substringBeforeLast('.') else name
            return number
                .findAll(base)
                .lastOrNull()
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        }
    }
}

/** PDF pages by the system renderer (RF-13, ADR-011): Android PdfRenderer, the browser pdf.js. */
interface PdfPages {
    suspend fun pageCount(path: String): Int

    suspend fun render(
        path: String,
        page: Int,
    ): ByteArray
}
