package com.tiagohs.hqr.presentation.feed

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * RF-115: a favorite source is one whose latest works are in the feed. The star adds or takes out that feed item, so
 * the feed always opens with the chosen sources and the choice travels in the backup with the feed.
 */
class FavoriteSources(
    private val searches: SearchRepository,
) {
    enum class Result { ADDED, REMOVED, FEED_FULL }

    fun observe(): Flow<Set<SourceId>> =
        searches.observeFeed().map { items -> items.filter { it.savedSearch == null }.map { it.sourceId }.toSet() }

    suspend fun toggle(sourceId: SourceId): Result {
        val item = searches.observeFeed().first().firstOrNull { it.sourceId == sourceId && it.savedSearch == null }
        return when {
            item != null -> {
                searches.removeFromFeed(item.id)
                Result.REMOVED
            }
            searches.addToFeed(sourceId, null) -> Result.ADDED
            else -> Result.FEED_FULL
        }
    }
}
