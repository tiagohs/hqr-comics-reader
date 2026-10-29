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
    data class BrowseSource(
        val sourceId: String,
    ) : Route

    @Serializable
    data class Manga(
        val id: Long,
    ) : Route

    @Serializable
    data class Reader(
        val mangaId: Long,
        val chapterId: Long,
        val page: Int = -1,
    ) : Route
}
