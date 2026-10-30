package com.tiagohs.hqr.sources.local

/**
 * CBR and CB7 (RF-13, ADR-011): opened only where the platform has the libraries (Android). These formats can't be read
 * page by page from the folder, so each archive is unpacked once to a private folder and read from there.
 */
interface ArchiveUnpacker {
    /** The files inside, folders included in the name ("a/b.jpg"), in the archive's order. */
    suspend fun names(path: String): List<String>

    suspend fun read(
        path: String,
        name: String,
    ): ByteArray

    /** The first page, for the cover; kept apart so a library of CBRs doesn't unpack every archive again. */
    suspend fun cover(path: String): ByteArray? = names(path).filter(::isPage).minWithOrNull(NaturalOrder)?.let { read(path, it) }
}
