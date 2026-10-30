package com.tiagohs.hqr.data.repository

import androidx.room3.withWriteTransaction
import com.tiagohs.hqr.data.db.FeedItemEntity
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.RecentSearchEntity
import com.tiagohs.hqr.data.db.SavedSearchEntity
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.FeedItem
import com.tiagohs.hqr.domain.repository.SavedSearch
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.SourceFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** Saved searches, feed and recent searches in Room (RF-24, RF-25, RF-27). */
class RoomSearchRepository(
    private val db: HqrDatabase,
    private val clock: Clock = Clock.System,
) : SearchRepository {
    private val dao = db.searchDao()
    private val json = Json { ignoreUnknownKeys = true }

    private fun SavedSearchEntity.toModel() =
        SavedSearch(
            id,
            SourceId(sourceId),
            name,
            query,
            runCatching { json.decodeFromString<List<SourceFilter>>(filters) }.getOrDefault(emptyList()),
        )

    override fun observeSaved(sourceId: SourceId): Flow<List<SavedSearch>> =
        dao.observeSaved().map { list -> list.filter { it.sourceId == sourceId.value }.map { it.toModel() } }

    override fun observeAllSaved(): Flow<List<SavedSearch>> = dao.observeSaved().map { list -> list.map { it.toModel() } }

    override suspend fun save(
        sourceId: SourceId,
        name: String,
        query: String,
        filters: List<SourceFilter>,
    ): Long =
        dao.insertSaved(
            SavedSearchEntity(sourceId = sourceId.value, name = name.trim(), query = query.trim(), filters = json.encodeToString(filters)),
        )

    override suspend fun rename(
        id: Long,
        name: String,
    ) {
        val current = dao.listSaved().firstOrNull { it.id == id } ?: return
        if (name.isNotBlank()) dao.upsertSaved(current.copy(name = name.trim()))
    }

    override suspend fun delete(id: Long) {
        db.withWriteTransaction {
            dao.listFeed().filter { it.savedSearchId == id }.forEach { dao.deleteFeed(it.id) }
            dao.deleteSaved(id)
        }
    }

    override fun observeFeed(): Flow<List<FeedItem>> =
        combine(dao.observeFeed(), dao.observeSaved()) { feed, saved ->
            val byId = saved.associateBy { it.id }
            feed.mapNotNull { item ->
                val search = item.savedSearchId?.let { byId[it] ?: return@mapNotNull null }
                FeedItem(item.id, SourceId(item.sourceId), search?.toModel())
            }
        }

    override suspend fun addToFeed(
        sourceId: SourceId,
        savedSearchId: Long?,
    ): Boolean =
        db.withWriteTransaction {
            val feed = dao.listFeed()
            val present = feed.any { it.sourceId == sourceId.value && it.savedSearchId == savedSearchId }
            if (present || feed.size >= SearchRepository.MAX_FEED) {
                false
            } else {
                dao.insertFeed(
                    FeedItemEntity(
                        sourceId = sourceId.value,
                        savedSearchId = savedSearchId,
                        sortOrder =
                            (feed.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    ),
                )
                true
            }
        }

    override suspend fun removeFromFeed(id: Long) = dao.deleteFeed(id)

    override fun observeRecent(): Flow<List<String>> = dao.observeRecent().map { list -> list.map { it.query } }

    override suspend fun addRecent(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        dao.upsertRecent(RecentSearchEntity(text, clock.now().toEpochMilliseconds()))
        dao.trimRecent()
    }

    override suspend fun removeRecent(query: String) = dao.deleteRecent(query)
}
