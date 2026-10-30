package com.tiagohs.hqr.domain.repository

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceFilter
import kotlinx.coroutines.flow.Flow

/** A search kept by name for one source: text and filters (RF-24). */
data class SavedSearch(
    val id: Long,
    val sourceId: SourceId,
    val name: String,
    val query: String,
    val filters: List<SourceFilter>,
)

/** One row of the feed: a source's latest, or one of its saved searches (RF-25). */
data class FeedItem(
    val id: Long,
    val sourceId: SourceId,
    val savedSearch: SavedSearch?,
)

/** Saved searches, the feed and the recent global searches (RF-24, RF-25, RF-27). */
interface SearchRepository {
    fun observeSaved(sourceId: SourceId): Flow<List<SavedSearch>>

    /** Every source's saved searches, for building the feed (SCR-EXP-004). */
    fun observeAllSaved(): Flow<List<SavedSearch>>

    suspend fun save(
        sourceId: SourceId,
        name: String,
        query: String,
        filters: List<SourceFilter>,
    ): Long

    suspend fun rename(
        id: Long,
        name: String,
    )

    /** Also leaves the feed. */
    suspend fun delete(id: Long)

    fun observeFeed(): Flow<List<FeedItem>>

    /** False when the feed already has [MAX_FEED] items or this one. */
    suspend fun addToFeed(
        sourceId: SourceId,
        savedSearchId: Long?,
    ): Boolean

    suspend fun removeFromFeed(id: Long)

    /** RF-27: newest first, at most [MAX_RECENT]. */
    fun observeRecent(): Flow<List<String>>

    suspend fun addRecent(query: String)

    suspend fun removeRecent(query: String)

    companion object {
        const val MAX_FEED = 5
        const val MAX_RECENT = 10
    }
}
