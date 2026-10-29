package com.tiagohs.hqr.sources.local

/** One entry of the chosen folder. [path] is relative to the folder root and is the work key (RF-90). */
data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModifiedEpochMs: Long = 0,
)

/**
 * Read-only access to the folder the user picked (RF-16): Android through the Storage Access Framework, the web
 * through the browser storage (OPFS), the JVM through the file system in tests. Nothing here writes.
 */
interface FileTree {
    /** True when a folder was chosen and is still readable. */
    suspend fun isReady(): Boolean

    /** Children of [path] ("" is the root), folders and files. */
    suspend fun children(path: String): List<FileNode>

    suspend fun size(path: String): Long

    /** Reads [length] bytes starting at [offset] (archives are read by ranges, never whole into memory). */
    suspend fun read(
        path: String,
        offset: Long,
        length: Int,
    ): ByteArray
}
