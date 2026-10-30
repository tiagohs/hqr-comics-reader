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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * The pages of downloaded chapters, one folder per chapter id, read only by HQR (RF-65). One operation at a time: the
 * queue writes while the screens list and read.
 */
class DownloadFiles(
    private val fs: FileSystem,
    private val root: Path,
) {
    private val lock = Mutex()

    fun dir(chapterId: Long): Path = root / chapterId.toString()

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

    /** Paths of the pages, in order; empty when the chapter has none. */
    suspend fun pages(chapterId: Long): List<Path> =
        lock.withLock {
            if (!fs.exists(
                    dir(chapterId),
                )
            ) {
                emptyList()
            } else {
                fs.list(dir(chapterId)).filter { !it.name.endsWith(".tmp") }.sortedBy { it.name }
            }
        }

    suspend fun read(path: Path): ByteArray = lock.withLock { fs.read(path) { readByteArray() } }

    suspend fun delete(chapterId: Long) = lock.withLock { fs.deleteRecursively(dir(chapterId)) }

    suspend fun usedBytes(): Long =
        lock.withLock {
            if (!fs.exists(root)) 0 else fs.listRecursively(root).sumOf { fs.metadataOrNull(it)?.size ?: 0L }
        }

    /** The address a page of a downloaded chapter carries to the image loader. */
    fun address(path: Path): String = SCHEME + path.toString()

    /** The file behind an address, only under the downloads folder. */
    fun pathOf(address: String): Path? =
        address
            .takeIf { it.startsWith(SCHEME) }
            ?.removePrefix(SCHEME)
            ?.toPath()
            ?.normalized()
            ?.takeIf { it.toString().startsWith(root.normalized().toString() + "/") }

    companion object {
        const val SCHEME = "download:"
        private const val PAD = 4
    }
}

/**
 * The queue of ADR-009: rows in `download`, run one chapter at a time by [run]; a paused or cancelled chapter keeps the
 * pages already saved and continues from them (RF-62 to RF-66).
 */
class DownloadManager(
    db: HqrDatabase,
    private val files: DownloadFiles,
    private val scheduler: DownloadScheduler,
    private val pagesOf: suspend (chapterId: Long) -> List<SourcePage>,
    private val bytesOf: suspend (chapterId: Long, page: SourcePage) -> ByteArray,
    /** RF-66: the "apagar depois de lido" option. */
    private val deleteAfterRead: () -> Boolean = { false },
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
            if (current == null || current.state == ERROR) {
                dao.upsert(
                    current?.copy(state = QUEUED, error = null)
                        ?: DownloadEntity(id, QUEUED, null, FORMAT, ++order, 0, 0, null),
                )
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
        queue.forEachIndexed { i, row -> dao.upsert(row.copy(queueOrder = orders[i])) }
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
        return files.pages(chapterId).mapIndexed { i, path -> SourcePage(i, files.address(path)) }.takeIf { it.isNotEmpty() }
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
            if (download(next.chapterId) == Outcome.NO_NETWORK) {
                dao.setStates(listOf(QUEUED), WAITING)
                return true
            }
        }
    }

    private enum class Outcome { DONE, STOPPED, FAILED, NO_NETWORK }

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
        val pages = pagesOf(id)
        var done = pages.count { files.has(id, it.index) }
        var running = dao.progress(id, done, pages.size) > 0
        for (page in pages.filter { !files.has(id, it.index) }) {
            val bytes = if (running) bytesOf(id, page) else break
            running = dao.get(id) != null
            if (running) {
                files.write(id, page.index, bytes)
                done++
                running = dao.progress(id, done, pages.size) > 0
            }
        }
        if (running && dao.finish(id, files.dir(id).toString()) > 0) return Outcome.DONE
        // Paused or removed meanwhile: the saved pages stay, unless the chapter left the list.
        if (dao.get(id) == null) files.delete(id)
        return Outcome.STOPPED
    }

    companion object {
        const val QUEUED = "QUEUED"
        const val RUNNING = "RUNNING"
        const val PAUSED = "PAUSED"
        const val WAITING = "WAITING_WIFI"
        const val DONE = "DONE"
        const val ERROR = "ERROR"
        private const val FORMAT = "images"
        private val PENDING = setOf(QUEUED, RUNNING, WAITING)
    }
}
