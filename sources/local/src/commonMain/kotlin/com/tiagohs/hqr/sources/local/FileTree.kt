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

/** What an import brought in and what it refused, with the file names (RF-110). */
data class ImportResult(
    val imported: List<String>,
    val refused: List<String>,
)

/** Used and available bytes of the browser storage (RF-110). */
data class StorageUsage(
    val used: Long,
    val quota: Long,
)

/**
 * RF-110: in the browser, files are imported into the app's storage (by picker or by dropping them on the window)
 * instead of reading a folder. CBR is refused with its reason.
 */
interface ImportedFiles {
    /** Grows on every import or removal, so the screens reload. */
    val changes: kotlinx.coroutines.flow.StateFlow<Int>

    /** Results of files dropped on the window. */
    val dropped: kotlinx.coroutines.flow.Flow<ImportResult>

    suspend fun pick(): ImportResult

    suspend fun usage(): StorageUsage?

    /** Asks the browser not to evict the files when space is short. */
    suspend fun persist(): Boolean
}
