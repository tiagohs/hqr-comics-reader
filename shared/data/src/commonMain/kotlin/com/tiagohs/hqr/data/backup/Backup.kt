package com.tiagohs.hqr.data.backup

import kotlinx.serialization.Serializable

/**
 * The `.hqrbackup` content (ADR-010): JSON, gzip outside, the same on Android and iOS (RF-74). Works are identified
 * by source and key (RF-90), chapters by key; never a database id, never a password (RF-72).
 */
@Serializable
data class Backup(
    val schemaVersion: Int = SCHEMA_VERSION,
    val createdAtEpochMs: Long,
    val app: String,
    val categories: List<BackupCategory> = emptyList(),
    val mangas: List<BackupManga> = emptyList(),
    val servers: List<BackupServer> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupCategory(
    val name: String,
    val order: Int,
)

@Serializable
data class BackupManga(
    val sourceId: String,
    /** The name the source had, for works of a source this app doesn't have (RF-73). */
    val sourceName: String,
    val key: String,
    val title: String,
    val url: String? = null,
    val coverUrl: String? = null,
    val inLibrary: Boolean,
    val addedAtEpochMs: Long? = null,
    val categories: List<String> = emptyList(),
    val chapters: List<BackupChapter> = emptyList(),
    val reader: BackupReaderSettings? = null,
)

@Serializable
data class BackupChapter(
    val key: String,
    val number: Double,
    val title: String? = null,
    val read: Boolean = false,
    val page: Int = 0,
    val pageCount: Int? = null,
    val readAtEpochMs: Long? = null,
)

@Serializable
data class BackupReaderSettings(
    val mode: String? = null,
    val scale: String? = null,
    val tapZones: String? = null,
    val background: String? = null,
    val doublePage: Boolean? = null,
    val cropBorders: Boolean? = null,
)

/** A server without its password: after restoring, the user types it again (RF-72). */
@Serializable
data class BackupServer(
    val sourceId: String,
    val kind: String,
    val name: String,
    val url: String,
    val username: String? = null,
)
