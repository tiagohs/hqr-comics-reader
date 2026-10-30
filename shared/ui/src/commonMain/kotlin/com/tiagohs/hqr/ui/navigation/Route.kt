package com.tiagohs.hqr.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Destinations. Arguments survive process death through the navigation state (RF-88, ADR-018); the short names are
 * the browser addresses (RF-113: `#library`, `#manga/12`).
 */
sealed interface Route {
    @Serializable
    @SerialName("library")
    data object Library : Route

    @Serializable
    @SerialName("explore")
    data object Explore : Route

    @Serializable
    @SerialName("history")
    data object History : Route

    @Serializable
    @SerialName("settings")
    data object Settings : Route

    @Serializable
    @SerialName("categories")
    data object Categories : Route

    /** [query] opens the source already searching (from global search, RF-23). */
    @Serializable
    @SerialName("browse-source")
    data class BrowseSource(
        val sourceId: String,
        val query: String? = null,
    ) : Route

    @Serializable
    @SerialName("search")
    data object Search : Route

    @Serializable
    @SerialName("sources")
    data object Sources : Route

    @Serializable
    @SerialName("downloads")
    data object Downloads : Route

    /** SCR-SRC-002: MangaDex's own settings (RF-06). */
    @Serializable
    @SerialName("source-settings")
    data object SourceSettings : Route

    /** SCR-WEB-001: the user's bridge, only in the browser (RF-108). */
    @Serializable
    @SerialName("bridge")
    data object Bridge : Route

    @Serializable
    @SerialName("backup")
    data object Backup : Route

    @Serializable
    @SerialName("about")
    data object About : Route

    @Serializable
    @SerialName("update")
    data object Update : Route

    /** [id] edits a saved server; null adds one. */
    @Serializable
    @SerialName("add-server")
    data class AddServer(
        val id: String? = null,
    ) : Route

    @Serializable
    @SerialName("server-catalog")
    data class ServerCatalog(
        val sourceId: String,
    ) : Route

    @Serializable
    @SerialName("manga")
    data class Manga(
        val id: Long,
    ) : Route

    /** [page] is [RESUME] to continue where the database says; an explicit page only for "read again" and jumps. */
    @Serializable
    @SerialName("reader")
    data class Reader(
        val mangaId: Long,
        val chapterId: Long,
        val page: Int = RESUME,
    ) : Route
}

/** Reader page meaning "where the saved progress is" (RF-43, RF-88). */
const val RESUME: Int = -1
