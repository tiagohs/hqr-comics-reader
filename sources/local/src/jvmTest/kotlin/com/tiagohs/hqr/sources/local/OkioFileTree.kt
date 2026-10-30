package com.tiagohs.hqr.sources.local

import okio.FileSystem
import okio.Path

/** FileTree over an okio file system: the real disk or a fake one in tests. */
class OkioFileTree(
    private val fs: FileSystem,
    private val root: Path,
) : FileTree {
    override suspend fun isReady(): Boolean = fs.exists(root)

    override suspend fun children(path: String): List<FileNode> {
        val dir = if (path.isEmpty()) root else root / path
        return fs.list(dir).map { child ->
            val meta = fs.metadata(child)
            FileNode(
                path = child.relativeTo(root).toString(),
                name = child.name,
                isDirectory = meta.isDirectory,
                size = meta.size ?: 0,
                lastModifiedEpochMs = meta.lastModifiedAtMillis ?: 0,
            )
        }
    }

    override suspend fun size(path: String): Long = fs.metadata(root / path).size ?: 0

    override suspend fun read(
        path: String,
        offset: Long,
        length: Int,
    ): ByteArray =
        fs.openReadOnly(root / path).use { handle ->
            val out = ByteArray(length)
            var done = 0
            while (done < length) {
                val n = handle.read(offset + done, out, done, length - done)
                if (n <= 0) break
                done += n
            }
            if (done == length) out else out.copyOf(done)
        }
}
