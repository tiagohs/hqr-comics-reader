package com.tiagohs.hqr.web

import com.tiagohs.hqr.sources.local.FileNode
import com.tiagohs.hqr.sources.local.FileTree

/** Files imported into the browser storage (RF-110, FEAT-052); empty until the import arrives. */
class BrowserFileTree : FileTree {
    override suspend fun isReady(): Boolean = false

    override suspend fun children(path: String): List<FileNode> = emptyList()

    override suspend fun size(path: String): Long = 0

    override suspend fun read(
        path: String,
        offset: Long,
        length: Int,
    ): ByteArray = ByteArray(0)
}
