package com.tiagohs.hqr.data.download

import com.tiagohs.hqr.data.db.DownloadEntity
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.source.SourceException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * The pages of downloaded chapters, one folder per chapter id — or one `.cbz` when the user asked for it (RF-67) — read
 * only by HQR (RF-65). One operation at a time: the queue writes while the screens list and read.
 */
class DownloadFiles(
    private val fs: FileSystem,
    private val root: Path,
) {
    private val lock = Mutex()

    fun dir(chapterId: Long): Path = root / chapterId.toString()

    /** The chapter as a single file (RF-67). */
    fun cbz(chapterId: Long): Path = root / "$chapterId.cbz"

    private fun page(
        chapterId: Long,
        index: Int,
    ): Path = dir(chapterId) / index.toString().padStart(PAD, '0')

    suspend fun has(
        chapterId: Long,
        index: Int,
    ): Boolean = lock.withLock { fs.exists(page(chapterId, index)) }

    suspend fun write(
        chapterId: Long,
        index: Int,
        bytes: ByteArray,
    ) = lock.withLock {
        fs.createDirectories(dir(chapterId))
        val target = page(chapterId, index)
        val tmp = target.parent!! / "${target.name}.tmp"
        fs.write(tmp) { write(bytes) }
        fs.atomicMove(tmp, target)
    }

    private fun folderPages(chapterId: Long): List<Path> =
        if (!fs.exists(dir(chapterId))) {
            emptyList()
        } else {
            fs.list(dir(chapterId)).filter { !it.name.endsWith(".tmp") }.sortedBy { it.name }
        }

    /** The addresses of the pages for the image loader, in order; empty when the chapter has none. */
    suspend fun addresses(chapterId: Long): List<String> =
        lock.withLock {
            val cbz = cbz(chapterId)
            if (fs.exists(cbz)) {
                Cbz.entries(fs, cbz).map { "$SCHEME$cbz$ENTRY${it.name}" }
            } else {
                folderPages(chapterId).map { SCHEME + it.toString() }
            }
        }

    /** RF-67: the finished chapter becomes one `.cbz` (pages named with their image type) and the folder goes. */
    suspend fun pack(chapterId: Long) =
        lock.withLock {
            val pages = folderPages(chapterId)
            if (pages.isEmpty()) return@withLock
            val target = cbz(chapterId)
            val tmp = cbzTmp(chapterId)
            try {
                Cbz.write(
                    fs,
                    tmp,
                    pages.size,
                    read = { fs.read(pages[it]) { readByteArray() } },
                    name = { i, bytes -> i.toString().padStart(PAD, '0') + "." + imageExtension(bytes) },
                )
                fs.atomicMove(tmp, target)
            } finally {
                // A pack cut short (disk full, process stopped) leaves no half file behind.
                fs.delete(tmp, mustExist = false)
            }
            fs.deleteRecursively(dir(chapterId))
        }

    private fun cbzTmp(chapterId: Long): Path = root / "$chapterId.cbz.tmp"

    /** RF-68: what the chapter's pages take, the room a pack needs. */
    suspend fun folderBytes(chapterId: Long): Long = lock.withLock { folderPages(chapterId).sumOf { fs.metadataOrNull(it)?.size ?: 0L } }

    /** RF-67: the chapter's `.cbz` written to [sink] (the copy in the chosen folder). */
    suspend fun copyCbz(
        chapterId: Long,
        sink: okio.Sink,
    ) = lock.withLock {
        fs.source(cbz(chapterId)).use { source -> sink.buffer().use { it.writeAll(source) } }
    }

    /** The bytes behind an address, only under the downloads folder; null for any other address. */
    suspend fun bytes(address: String): ByteArray? {
        val (path, entry) = locate(address) ?: return null
        return lock.withLock {
            if (entry == null) {
                fs.read(path) { readByteArray() }
            } else {
                Cbz.entries(fs, path).firstOrNull { it.name == entry }?.let { Cbz.read(fs, path, it) }
            }
        }
    }

    suspend fun delete(chapterId: Long) =
        lock.withLock {
            fs.deleteRecursively(dir(chapterId))
            fs.delete(cbz(chapterId), mustExist = false)
            fs.delete(cbzTmp(chapterId), mustExist = false)
        }

    suspend fun usedBytes(): Long =
        lock.withLock {
            if (!fs.exists(root)) 0 else fs.listRecursively(root).sumOf { fs.metadataOrNull(it)?.size ?: 0L }
        }

    /** The file behind an address (and the page inside it, for a `.cbz`), only under the downloads folder. */
    fun locate(address: String): Pair<Path, String?>? {
        if (!address.startsWith(SCHEME)) return null
        val rest = address.removePrefix(SCHEME)
        val entry = rest.substringAfter(ENTRY, "").ifEmpty { null }
        val path = rest.substringBefore(ENTRY).toPath().normalized()
        return (path to entry).takeIf { path.toString().startsWith(root.normalized().toString() + "/") }
    }

    companion object {
        const val SCHEME = "download:"
        private const val ENTRY = "!/"
        private const val PAD = 4
    }
}

/**
 * The queue of ADR-009: rows in `download`, run one chapter at a time by [run]; a paused or cancelled chapter keeps the
 * pages already saved and continues from them (RF-62 to RF-66).
 */
@Suppress("TooManyFunctions") // The Downloads contract plus the run of the queue.
class DownloadManager(
    db: HqrDatabase,
    private val files: DownloadFiles,
    private val scheduler: DownloadScheduler,
    private val pagesOf: suspend (chapterId: Long) -> List<SourcePage>,
    private val bytesOf: suspend (chapterId: Long, page: SourcePage) -> ByteArray,
    /** RF-66: the "apagar depois de lido" option. */
    private val deleteAfterRead: () -> Boolean = { false },
    /** RF-67: keep each finished chapter as one `.cbz`. */
    private val saveAsCbz: () -> Boolean = { false },
    /** RF-67: the folder that also gets a `.cbz` of each finished chapter; null for none. */
    private val folder: () -> String? = { null },
    private val export: DownloadExport? = null,
    /** RF-68: free bytes where the downloads are; null where the platform can't tell. */
    private val freeBytes: () -> Long? = { null },
) : Downloads {
    private val dao = db.downloadDao()

    override fun observe(): Flow<List<DownloadItem>> =
        dao.observeRows().map { rows ->
            rows.map { r ->
                DownloadItem(
                    chapterId = r.chapterId,
                    mangaId = r.mangaId,
                    mangaTitle = r.mangaTitle,
                    chapterLabel = r.chapterTitle?.takeIf { it.isNotBlank() } ?: r.number.chapterLabel(),
                    state = DownloadState.valueOf(r.state),
                    pagesDone = r.pagesDone,
                    pagesTotal = r.pagesTotal,
                    error = r.error,
                )
            }
        }

    override suspend fun enqueue(chapterIds: List<Long>) {
        var order = dao.maxOrder()
        chapterIds.forEach { id ->
            val current = dao.get(id)
            when {
                current == null -> dao.upsert(DownloadEntity(id, QUEUED, null, FORMAT, ++order, 0, 0, null))
                current.state == ERROR -> dao.retry(id)
            }
        }
        scheduler.schedule(restart = false)
    }

    override suspend fun pauseAll() = dao.setStates(listOf(QUEUED, RUNNING, WAITING), PAUSED)

    override suspend fun resumeAll() {
        dao.setStates(listOf(PAUSED, ERROR), QUEUED)
        scheduler.schedule(restart = false)
    }

    override suspend fun networkRuleChanged() {
        dao.setStates(listOf(WAITING), QUEUED)
        if (dao.list().any { it.state in PENDING }) scheduler.schedule(restart = true)
    }

    override suspend fun resumePending() {
        if (dao.list().any { it.state in PENDING }) scheduler.schedule(restart = false)
    }

    override suspend fun chaptersRead(chapterIds: Collection<Long>) {
        if (!deleteAfterRead() || chapterIds.isEmpty()) return
        remove(dao.readDone(chapterIds))
    }

    override suspend fun move(
        chapterId: Long,
        by: Int,
    ) {
        val queue = dao.list().filter { it.state != DONE }.toMutableList()
        val from = queue.indexOfFirst { it.chapterId == chapterId }
        val to = from + by
        if (from < 0 || to !in queue.indices) return
        queue.add(to, queue.removeAt(from))
        val orders = queue.map { it.queueOrder }.sorted()
        queue.forEachIndexed { i, row -> dao.reorder(row.chapterId, orders[i]) }
    }

    override suspend fun remove(chapterIds: Collection<Long>) {
        chapterIds.forEach { id ->
            dao.delete(id)
            files.delete(id)
        }
    }

    override suspend fun removeAll() = remove(dao.list().map { it.chapterId })

    override suspend fun usedBytes(): Long = files.usedBytes()

    /** The pages of a downloaded chapter as addresses for the image loader; null when it is not fully downloaded. */
    suspend fun offlinePages(chapterId: Long): List<SourcePage>? {
        if (dao.get(chapterId)?.state != DONE) return null
        return files.addresses(chapterId).mapIndexed { i, address -> SourcePage(i, address) }.takeIf { it.isNotEmpty() }
    }

    /**
     * Runs the queue until nothing is left to do. [allowed] answers whether the network can be used now (RF-64: false on
     * a metered network with "só no Wi-Fi"). Returns true when chapters were left waiting for the network, so the
     * executor tries again later (RF-63).
     */
    suspend fun run(allowed: () -> Boolean): Boolean {
        // A chapter left RUNNING by a killed process continues (RF-63).
        while (true) {
            val next = dao.list().firstOrNull { it.state in PENDING } ?: return false
            if (!allowed()) {
                dao.setStates(listOf(QUEUED, RUNNING), WAITING)
                return true
            }
            when (download(next.chapterId)) {
                Outcome.NO_NETWORK -> {
                    dao.setStates(listOf(QUEUED), WAITING)
                    return true
                }
                // RF-68: the queue stays paused until the user resumes it.
                Outcome.LOW_SPACE -> return false
                else -> Unit
            }
        }
    }

    private enum class Outcome { DONE, STOPPED, FAILED, NO_NETWORK, LOW_SPACE }

    private suspend fun download(id: Long): Outcome =
        try {
            if (dao.start(id) == 0) Outcome.STOPPED else fetchPages(id)
        } catch (e: CancellationException) {
            // The executor was stopped (system, network rule): the chapter goes back to the queue for the next run.
            withContext(NonCancellable) { dao.stop(id, QUEUED, null) }
            throw e
        } catch (e: SourceException) {
            if (e.kind == SourceException.Kind.NETWORK) {
                dao.stop(id, WAITING, null)
                Outcome.NO_NETWORK
            } else {
                dao.stop(id, ERROR, e.message ?: e.kind.name)
                Outcome.FAILED
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            dao.stop(id, ERROR, e.message ?: e::class.simpleName)
            Outcome.FAILED
        }

    /** The missing pages, one by one; a page that arrives after a pause is kept, after a removal it goes. */
    private suspend fun fetchPages(id: Long): Outcome {
        // A chapter without pages would be "downloaded" with nothing to read.
        val pages = pagesOf(id).ifEmpty { throw SourceException("Capítulo sem páginas", SourceException.Kind.NOT_FOUND) }
        var done = pages.count { files.has(id, it.index) }
        var running = dao.progress(id, done, pages.size) > 0
        for (page in pages.filter { !files.has(id, it.index) }) {
            if (running && lowSpace(id)) return Outcome.LOW_SPACE
            val bytes = if (running) bytesOf(id, page) else break
            running = dao.get(id) != null
            if (running) {
                files.write(id, page.index, bytes)
                done++
                running = dao.progress(id, done, pages.size) > 0
            }
        }
        return if (running) finish(id) else stopped(id)
    }

    /** RF-67: packed before it is "downloaded", so the reader never gets addresses of a folder about to go. */
    private suspend fun finish(id: Long): Outcome {
        val packs = saveAsCbz() || folder() != null
        return when {
            packs && lowSpace(id, needed = files.folderBytes(id)) -> Outcome.LOW_SPACE
            // Paused or removed right before: the pages stay as they are, for the next run.
            dao.get(id)?.state != RUNNING -> stopped(id)
            else -> {
                if (packs) files.pack(id)
                if (dao.finish(id, (if (packs) files.cbz(id) else files.dir(id)).toString()) == 0) {
                    stopped(id)
                } else {
                    export(id)
                    Outcome.DONE
                }
            }
        }
    }

    /** Paused or removed meanwhile: the saved pages stay, unless the chapter left the list. */
    private suspend fun stopped(id: Long): Outcome {
        if (dao.get(id) == null) files.delete(id)
        return Outcome.STOPPED
    }

    /** RF-68: short of space, the running chapter and the rest of the queue pause, with the reason. */
    private suspend fun lowSpace(
        id: Long,
        needed: Long = 0,
    ): Boolean {
        val free = freeBytes() ?: return false
        if (free - needed >= DownloadItem.MIN_FREE_BYTES) return false
        dao.stop(id, PAUSED, DownloadItem.LOW_SPACE)
        dao.pauseForSpace(listOf(QUEUED, WAITING), DownloadItem.LOW_SPACE)
        return true
    }

    /** RF-67: a copy of the finished chapter's `.cbz` in the chosen folder. */
    private suspend fun export(id: Long) {
        val target = folder()
        val export = export
        val row = dao.observeRows().first().firstOrNull { it.chapterId == id }
        if (target == null || export == null || row == null) return
        // "012 - Title [Group]": numbers padded so other readers sort the files in order; the group tells apart two
        // chapters with the same number (another translation), which would otherwise replace each other.
        val number = row.number.chapterLabel().padStart(NUMBER_PAD, '0')
        val title = row.chapterTitle?.takeIf { it.isNotBlank() }?.let { "$number - $it" } ?: number
        val name = row.group?.takeIf { it.isNotBlank() }?.let { "$title [$it]" } ?: title
        // A copy that fails (folder gone, permission revoked) never undoes the download: HQR reads its own file.
        try {
            export.save(target, safeName(row.mangaTitle), "${safeName(name)}.cbz") { sink -> files.copyCbz(id, sink) }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            Unit
        }
    }

    companion object {
        const val QUEUED = "QUEUED"
        const val RUNNING = "RUNNING"
        const val PAUSED = "PAUSED"
        const val WAITING = "WAITING_WIFI"
        const val DONE = "DONE"
        const val ERROR = "ERROR"
        private const val FORMAT = "images"
        private const val NUMBER_PAD = 3
        private val PENDING = setOf(QUEUED, RUNNING, WAITING)
    }
}

/** RF-67: where the copy of a finished chapter goes (Android: the folder picked with the system picker). */
fun interface DownloadExport {
    /** Writes [fileName] in a folder named [work] inside [folder]; false when it could not. */
    suspend fun save(
        folder: String,
        work: String,
        fileName: String,
        write: suspend (okio.Sink) -> Unit,
    ): Boolean
}

/** RF-68: the free space where the downloads are; null when unknown. */
fun interface FreeSpace {
    fun bytes(): Long?
}

/** A file or folder name any system accepts. */
internal fun safeName(name: String): String =
    name
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        .trim()
        .trimEnd('.')
        .take(MAX_NAME)
        .ifEmpty { "_" }

private const val MAX_NAME = 120
