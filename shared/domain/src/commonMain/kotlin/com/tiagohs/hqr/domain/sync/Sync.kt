package com.tiagohs.hqr.domain.sync

import kotlinx.serialization.Serializable

/** What travels between devices (RF-121); downloads, imported files, cache and device settings never do. */
enum class SyncKind(
    val wire: String,
) {
    MANGA("manga"),
    CHAPTER("chapter"),
    CATEGORY("category"),
    SAVED_SEARCH("saved_search"),
    FEED("feed"),
    SERVER("server"),
    SETTING("setting"),
    ;

    companion object {
        fun of(wire: String): SyncKind? = entries.firstOrNull { it.wire == wire }
    }
}

/** One item of the account, by kind and key (RF-90 identity: works by source and key, chapters by key). */
data class SyncItemId(
    val kind: SyncKind,
    val key: String,
)

/** A change that goes to or comes from the Worker; `changedAt` orders the versions of one item (RF-123). */
data class SyncChange(
    val kind: SyncKind,
    val key: String,
    val payload: String,
    val changedAt: Long,
    val deleted: Boolean,
) {
    val id: SyncItemId get() = SyncItemId(kind, key)
}

/** Keys made of parts that may contain any printable character, joined by the unit separator. */
object SyncKeys {
    private const val SEPARATOR = '\u001F'

    fun join(vararg parts: String): String = parts.joinToString(SEPARATOR.toString())

    fun split(key: String): List<String> = key.split(SEPARATOR)
}

@Serializable
data class MangaRecord(
    val title: String,
    val url: String? = null,
    val coverUrl: String? = null,
    val inLibrary: Boolean,
    val addedAt: Long? = null,
    val categories: List<String> = emptyList(),
    val reader: ReaderRecord? = null,
)

@Serializable
data class ReaderRecord(
    val mode: String? = null,
    val scale: String? = null,
    val tapZones: String? = null,
    val background: String? = null,
    val doublePage: Boolean? = null,
    val cropBorders: Boolean? = null,
)

/**
 * A chapter's progress and history. `progressAt` is when the page or the read mark last changed, `readAt` the last
 * reading (history), `unreadAt` when the user marked it unread and hasn't read it since.
 */
@Serializable
data class ChapterRecord(
    val number: Double,
    val title: String? = null,
    val read: Boolean = false,
    val page: Int = 0,
    val pageCount: Int? = null,
    val progressAt: Long = 0,
    val readAt: Long? = null,
    val unreadAt: Long? = null,
)

@Serializable
data class CategoryRecord(
    val name: String,
    val order: Int,
)

@Serializable
data class SavedSearchRecord(
    val query: String,
    val filters: String,
)

@Serializable
data class FeedRecord(
    val order: Int,
)

/** A server without its password (RF-72). */
@Serializable
data class ServerRecord(
    val kind: String,
    val name: String,
    val url: String,
    val username: String? = null,
)

/** The merge rules, the same on Android, web and iOS (ADR-029). */
object SyncRules {
    /** The `changedAt` of what a device had before its first sign-in: the account's own versions win (RF-124). */
    const val BEFORE_ACCOUNT = 1L

    /**
     * What changed on this device since the last cycle: new or different items, and removals as tombstones. Before the
     * first sync (`initial`), the device's items go with [BEFORE_ACCOUNT] so nothing in the account is overwritten;
     * chapters are merged field by field anyway.
     */
    fun diff(
        snapshot: Map<SyncItemId, String>,
        current: Map<SyncItemId, String>,
        now: Long,
        initial: Boolean,
    ): List<SyncChange> {
        val changed =
            current.filter { (id, payload) -> snapshot[id] != payload }.map { (id, payload) ->
                val at = if (initial && id.kind != SyncKind.CHAPTER) BEFORE_ACCOUNT else now
                SyncChange(id.kind, id.key, payload, at, deleted = false)
            }
        // A chapter never leaves on its own (its progress stays); only the other kinds become tombstones.
        val removed =
            snapshot.keys
                .filter { it !in current && it.kind != SyncKind.CHAPTER }
                .map { SyncChange(it.kind, it.key, "{}", now, deleted = true) }
        return changed + removed
    }

    /** Read → unread on this device can only be the user's action ("Marcar como não lido"): it is remembered. */
    fun withUnreadMark(
        previous: ChapterRecord?,
        current: ChapterRecord,
    ): ChapterRecord =
        when {
            current.read -> current.copy(unreadAt = null)
            previous?.read == true -> current.copy(unreadAt = current.progressAt)
            else -> current.copy(unreadAt = previous?.unreadAt)
        }

    /**
     * RF-123: read stays read unless the newer side was marked unread by the user; the page is the one of the most
     * recent reading; the last reading time is the latest of the two.
     */
    fun mergeChapter(
        local: ChapterRecord?,
        remote: ChapterRecord,
    ): ChapterRecord {
        if (local == null) return remote
        val newer = if (remote.progressAt > local.progressAt) remote else local
        val read = if (newer.unreadAt != null && !newer.read) false else local.read || remote.read
        val readAt = listOfNotNull(local.readAt, remote.readAt).maxOrNull()
        return newer.copy(
            title = newer.title ?: local.title ?: remote.title,
            read = read,
            pageCount = newer.pageCount ?: local.pageCount ?: remote.pageCount,
            readAt = readAt,
            unreadAt = if (read) null else newer.unreadAt,
        )
    }
}
