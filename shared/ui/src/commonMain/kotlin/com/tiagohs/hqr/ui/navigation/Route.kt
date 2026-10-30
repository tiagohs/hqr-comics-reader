package com.tiagohs.hqr.ui.navigation

import kotlinx.serialization.Serializable

/** Destinations. Arguments survive process death through the navigation state (RF-88, ADR-018). */
sealed interface Route {
    @Serializable
    data object Library : Route

    @Serializable
    data object Explore : Route

    @Serializable
    data object History : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Categories : Route

    /** [query] opens the source already searching (from global search, RF-23). */
    @Serializable
    data class BrowseSource(
        val sourceId: String,
        val query: String? = null,
    ) : Route

    @Serializable
    data object Search : Route

    @Serializable
    data class Manga(
        val id: Long,
    ) : Route

    /** [page] is [RESUME] to continue where the database says; an explicit page only for "read again" and jumps. */
    @Serializable
    data class Reader(
        val mangaId: Long,
        val chapterId: Long,
        val page: Int = RESUME,
    ) : Route
}

/** Reader page meaning "where the saved progress is" (RF-43, RF-88). */
const val RESUME: Int = -1
