package com.tiagohs.hqr.sources.opds

import com.tiagohs.hqr.sources.local.FileNode
import com.tiagohs.hqr.sources.local.FileTree
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/**
 * Where a book file downloaded from a server stays while it is read (RF-18): on disk in the app's cache on Android,
 * in memory in the browser. At most [KEPT] files: the chapter being read and the next one.
 */
interface BookStore {
    /** The file already kept for [key], if any. */
    fun open(key: String): FileTree?

    /** Saves the bytes [write] hands over, chunk by chunk; [write] returns false to give up (too big). */
    suspend fun save(
        key: String,
        write: suspend (sink: (ByteArray, Int) -> Unit) -> Boolean,
    ): FileTree?

    companion object {
        const val KEPT = 2
        const val PATH = "book.cbz"
    }
}

/** Book files in a cache folder, read by ranges, never whole in memory. */
class DiskBookStore(
    private val fs: FileSystem,
    private val dir: Path,
) : BookStore {
    private fun file(key: String): Path = dir / ("book-" + key.hashCode().toUInt().toString(HEX) + "-" + key.length)

    override fun open(key: String): FileTree? = file(key).takeIf { fs.exists(it) }?.let { DiskTree(fs, it) }

    override suspend fun save(
        key: String,
        write: suspend (sink: (ByteArray, Int) -> Unit) -> Boolean,
    ): FileTree? {
        fs.createDirectories(dir)
        val target = file(key)
        val tmp = dir / "${target.name}.tmp"
        val complete =
            fs.sink(tmp).buffer().use { out ->
                write { bytes, count -> out.write(bytes, 0, count) }
            }
        if (!complete) {
            fs.delete(tmp)
            return null
        }
        fs.atomicMove(tmp, target)
        // The oldest files go; the one just saved stays.
        fs
            .list(dir)
            .filter { !it.name.endsWith(".tmp") && it != target }
            .sortedByDescending { fs.metadataOrNull(it)?.lastModifiedAtMillis ?: 0 }
            .drop(BookStore.KEPT - 1)
            .forEach { fs.delete(it) }
        return DiskTree(fs, target)
    }

    private class DiskTree(
        private val fs: FileSystem,
        private val file: Path,
    ) : FileTree {
        override suspend fun isReady() = true

        override suspend fun children(path: String) = emptyList<FileNode>()

        override suspend fun size(path: String) = fs.metadata(file).size ?: 0L

        override suspend fun read(
            path: String,
            offset: Long,
            length: Int,
        ): ByteArray =
            fs.openReadOnly(file).use { handle ->
                val out = ByteArray(length)
                var total = 0
                while (total < length) {
                    val n = handle.read(offset + total, out, total, length - total)
                    if (n <= 0) break
                    total += n
                }
                if (total == length) out else out.copyOf(total)
            }
    }

    private companion object {
        const val HEX = 16
    }
}

/** Book files in memory, for the browser; the size limit of the source keeps them small. */
class MemoryBookStore : BookStore {
    private val files = LinkedHashMap<String, ByteArray>()

    override fun open(key: String): FileTree? = files[key]?.let(::MemoryTree)

    override suspend fun save(
        key: String,
        write: suspend (sink: (ByteArray, Int) -> Unit) -> Boolean,
    ): FileTree? {
        val out = okio.Buffer()
        if (!write { bytes, count -> out.write(bytes, 0, count) }) return null
        val bytes = out.readByteArray()
        files[key] = bytes
        while (files.size > BookStore.KEPT) files.remove(files.keys.first())
        return MemoryTree(bytes)
    }

    private class MemoryTree(
        private val bytes: ByteArray,
    ) : FileTree {
        override suspend fun isReady() = true

        override suspend fun children(path: String) = emptyList<FileNode>()

        override suspend fun size(path: String) = bytes.size.toLong()

        override suspend fun read(
            path: String,
            offset: Long,
            length: Int,
        ): ByteArray = bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + length))
    }
}
