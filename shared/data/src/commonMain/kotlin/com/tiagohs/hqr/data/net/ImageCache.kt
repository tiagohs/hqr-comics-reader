package com.tiagohs.hqr.data.net

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path

private const val MB = 1024L * 1024L
private const val MEMORY_CACHE_BYTES = 64 * MB
private const val DISK_CACHE_BYTES = 300 * MB

/** Bytes cache for pages and covers (RNF-23): an LRU on disk where there is a disk, in memory in the browser. */
interface ImageCache {
    suspend fun get(key: String): ByteArray?

    suspend fun put(
        key: String,
        bytes: ByteArray,
    )

    suspend fun size(): Long

    suspend fun clear()
}

class MemoryImageCache(
    private val maxBytes: Long = MEMORY_CACHE_BYTES,
) : ImageCache {
    private val mutex = Mutex()
    private val map = LinkedHashMap<String, ByteArray>()
    private var total = 0L

    override suspend fun get(key: String): ByteArray? =
        mutex.withLock {
            map.remove(key)?.also { map[key] = it }
        }

    override suspend fun put(
        key: String,
        bytes: ByteArray,
    ) = mutex.withLock {
        map.remove(key)?.let { total -= it.size }
        map[key] = bytes
        total += bytes.size
        while (total > maxBytes && map.isNotEmpty()) {
            val first = map.keys.first()
            total -= map.remove(first)?.size ?: 0
        }
    }

    override suspend fun size(): Long = mutex.withLock { total }

    override suspend fun clear() =
        mutex.withLock {
            map.clear()
            total = 0
        }
}

/** Disk LRU by last access (file modification time), trimmed to [maxBytes]. */
class DiskImageCache(
    private val fs: FileSystem,
    private val dir: Path,
    private val maxBytes: Long = DISK_CACHE_BYTES,
) : ImageCache {
    private val mutex = Mutex()

    private fun file(key: String): Path = dir / key.encodeUtf8().sha256().hex()

    override suspend fun get(key: String): ByteArray? =
        mutex.withLock {
            val path = file(key)
            if (!fs.exists(path)) return@withLock null
            fs.read(path) { readByteArray() }.also { bytes -> fs.write(path) { write(bytes) } }
        }

    override suspend fun put(
        key: String,
        bytes: ByteArray,
    ) = mutex.withLock {
        fs.createDirectories(dir)
        val tmp = dir / "${file(key).name}.tmp"
        fs.write(tmp) { write(bytes) }
        fs.atomicMove(tmp, file(key))
        trim()
    }

    private fun trim() {
        val files = fs.list(dir).map { it to fs.metadata(it) }
        var total = files.sumOf { it.second.size ?: 0 }
        if (total <= maxBytes) return
        for ((path, meta) in files.sortedBy { it.second.lastModifiedAtMillis ?: 0 }) {
            fs.delete(path)
            total -= meta.size ?: 0
            if (total <= maxBytes) break
        }
    }

    override suspend fun size(): Long = mutex.withLock { if (fs.exists(dir)) fs.list(dir).sumOf { fs.metadata(it).size ?: 0 } else 0 }

    override suspend fun clear() = mutex.withLock { if (fs.exists(dir)) fs.deleteRecursively(dir) }
}
