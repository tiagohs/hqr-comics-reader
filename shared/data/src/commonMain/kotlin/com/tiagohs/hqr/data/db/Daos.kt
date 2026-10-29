package com.tiagohs.hqr.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** One row of the library grid: the work plus its counters (RF-54, RF-55). */
data class LibraryRow(
    val id: Long,
    val unread: Int,
    val downloaded: Int,
    val total: Int,
    val lastReadAt: Long?,
    val latestUploadAt: Long?,
)

@Dao
interface MangaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(manga: MangaEntity): Long

    @Query("SELECT * FROM manga WHERE sourceId = :sourceId AND `key` = :key")
    suspend fun find(
        sourceId: String,
        key: String,
    ): MangaEntity?

    @Query("SELECT * FROM manga WHERE id = :id")
    suspend fun get(id: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE id = :id")
    fun observe(id: Long): Flow<MangaEntity?>

    @Query(
        """
        UPDATE manga SET url = :url, title = :title, alternativeTitles = :alternativeTitles, coverUrl = :coverUrl,
        authors = :authors, artists = :artists, status = :status, genres = :genres, language = :language,
        description = :description, kind = :kind, updatedAt = :updatedAt WHERE id = :id
        """,
    )
    suspend fun updateFromSource(
        id: Long,
        url: String?,
        title: String,
        alternativeTitles: String,
        coverUrl: String?,
        authors: String,
        artists: String,
        status: String,
        genres: String,
        language: String?,
        description: String?,
        kind: String,
        updatedAt: Long,
    )

    @Query("UPDATE manga SET inLibrary = :inLibrary, addedAt = :addedAt WHERE id = :id")
    suspend fun setInLibrary(
        id: Long,
        inLibrary: Boolean,
        addedAt: Long?,
    )

    @Query("UPDATE manga SET coverColor = :color WHERE id = :id")
    suspend fun setCoverColor(
        id: Long,
        color: Int?,
    )

    @Query("SELECT * FROM manga WHERE inLibrary = 1 ORDER BY title COLLATE NOCASE")
    fun observeLibrary(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT m.id AS id,
          (SELECT COUNT(DISTINCT c.number) FROM chapter c LEFT JOIN progress p ON p.chapterId = c.id
             WHERE c.mangaId = m.id AND COALESCE(p.read, 0) = 0) AS unread,
          (SELECT COUNT(*) FROM chapter c JOIN download d ON d.chapterId = c.id
             WHERE c.mangaId = m.id AND d.state = 'DONE') AS downloaded,
          (SELECT COUNT(DISTINCT c.number) FROM chapter c WHERE c.mangaId = m.id) AS total,
          (SELECT MAX(h.readAt) FROM history h JOIN chapter c ON c.id = h.chapterId WHERE c.mangaId = m.id) AS lastReadAt,
          (SELECT MAX(c.uploadedAt) FROM chapter c WHERE c.mangaId = m.id) AS latestUploadAt
        FROM manga m WHERE m.inLibrary = 1
        """,
    )
    fun observeLibraryRows(): Flow<List<LibraryRow>>

    @Query("SELECT `key` FROM manga WHERE inLibrary = 1 AND sourceId = :sourceId")
    fun observeLibraryKeys(sourceId: String): Flow<List<String>>

    @Query("SELECT * FROM manga")
    suspend fun all(): List<MangaEntity>
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapter WHERE mangaId = :mangaId ORDER BY number DESC, sourceOrder")
    suspend fun list(mangaId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapter WHERE mangaId = :mangaId ORDER BY number DESC, sourceOrder")
    fun observe(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapter WHERE id = :id")
    suspend fun get(id: Long): ChapterEntity?

    @Insert
    suspend fun insert(chapter: ChapterEntity): Long

    @Upsert
    suspend fun upsert(chapter: ChapterEntity)

    @Query("UPDATE chapter SET removedAtSource = :removed WHERE id = :id")
    suspend fun setRemoved(
        id: Long,
        removed: Boolean,
    )

    @Query("SELECT * FROM chapter")
    suspend fun all(): List<ChapterEntity>
}

@Dao
interface ProgressDao {
    @Query("SELECT p.* FROM progress p JOIN chapter c ON c.id = p.chapterId WHERE c.mangaId = :mangaId")
    fun observeForManga(mangaId: Long): Flow<List<ProgressEntity>>

    @Query("SELECT p.* FROM progress p JOIN chapter c ON c.id = p.chapterId WHERE c.mangaId = :mangaId")
    suspend fun listForManga(mangaId: Long): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE chapterId = :chapterId")
    suspend fun get(chapterId: Long): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>
}

/** One history row with the ids needed to load work and chapter (RF-59). */
data class HistoryRow(
    val chapterId: Long,
    val mangaId: Long,
    val readAt: Long,
)

@Dao
interface HistoryDao {
    @Upsert
    suspend fun upsert(entry: HistoryEntity)

    @Query("SELECT h.chapterId, c.mangaId, h.readAt FROM history h JOIN chapter c ON c.id = h.chapterId ORDER BY h.readAt DESC")
    fun observe(): Flow<List<HistoryRow>>

    @Query(
        """
        SELECT h.chapterId, c.mangaId, h.readAt FROM history h JOIN chapter c ON c.id = h.chapterId
        WHERE h.readAt = (SELECT MAX(h2.readAt) FROM history h2 JOIN chapter c2 ON c2.id = h2.chapterId WHERE c2.mangaId = c.mangaId)
        ORDER BY h.readAt DESC
        """,
    )
    fun observeLatestPerManga(): Flow<List<HistoryRow>>

    @Query("DELETE FROM history WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: Long)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("SELECT * FROM history")
    suspend fun all(): List<HistoryEntity>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sortOrder")
    fun observe(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder")
    suspend fun list(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE category SET name = :name WHERE id = :id")
    suspend fun rename(
        id: Long,
        name: String,
    )

    @Query("UPDATE category SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(
        id: Long,
        order: Int,
    )

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM manga_category WHERE mangaId = :mangaId")
    suspend fun forManga(mangaId: Long): List<MangaCategoryEntity>

    @Query("SELECT * FROM manga_category")
    fun observeAssignments(): Flow<List<MangaCategoryEntity>>

    @Query("SELECT * FROM manga_category")
    suspend fun allAssignments(): List<MangaCategoryEntity>

    @Query("DELETE FROM manga_category WHERE mangaId = :mangaId")
    suspend fun clearForManga(mangaId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(link: MangaCategoryEntity)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download ORDER BY queueOrder")
    fun observe(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download ORDER BY queueOrder")
    suspend fun list(): List<DownloadEntity>

    @Query("SELECT * FROM download WHERE chapterId = :chapterId")
    suspend fun get(chapterId: Long): DownloadEntity?

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query("DELETE FROM download WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: Long)

    @Query("SELECT COALESCE(MAX(queueOrder), 0) FROM download")
    suspend fun maxOrder(): Int
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM source")
    fun observe(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM source")
    suspend fun list(): List<SourceEntity>

    @Upsert
    suspend fun upsert(source: SourceEntity)

    @Query("DELETE FROM source WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM saved_search ORDER BY name COLLATE NOCASE")
    fun observeSaved(): Flow<List<SavedSearchEntity>>

    @Insert
    suspend fun insertSaved(search: SavedSearchEntity): Long

    @Upsert
    suspend fun upsertSaved(search: SavedSearchEntity)

    @Query("DELETE FROM saved_search WHERE id = :id")
    suspend fun deleteSaved(id: Long)

    @Query("SELECT * FROM feed_item ORDER BY sortOrder")
    fun observeFeed(): Flow<List<FeedItemEntity>>

    @Query("SELECT * FROM feed_item ORDER BY sortOrder")
    suspend fun listFeed(): List<FeedItemEntity>

    @Insert
    suspend fun insertFeed(item: FeedItemEntity): Long

    @Query("DELETE FROM feed_item WHERE id = :id")
    suspend fun deleteFeed(id: Long)

    @Query("UPDATE feed_item SET sortOrder = :order WHERE id = :id")
    suspend fun setFeedOrder(
        id: Long,
        order: Int,
    )

    @Query("SELECT * FROM recent_search ORDER BY searchedAt DESC LIMIT 10")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsertRecent(search: RecentSearchEntity)

    @Query("DELETE FROM recent_search WHERE `query` = :query")
    suspend fun deleteRecent(query: String)

    @Query("DELETE FROM recent_search WHERE `query` NOT IN (SELECT `query` FROM recent_search ORDER BY searchedAt DESC LIMIT 10)")
    suspend fun trimRecent()
}

@Dao
interface ServerDao {
    @Query("SELECT * FROM server ORDER BY name COLLATE NOCASE")
    fun observe(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM server")
    suspend fun list(): List<ServerEntity>

    @Upsert
    suspend fun upsert(server: ServerEntity)

    @Query("DELETE FROM server WHERE sourceId = :sourceId")
    suspend fun delete(sourceId: String)
}

@Dao
interface ReaderSettingsDao {
    @Query("SELECT * FROM manga_reader_settings WHERE mangaId = :mangaId")
    suspend fun get(mangaId: Long): MangaReaderSettingsEntity?

    @Upsert
    suspend fun upsert(settings: MangaReaderSettingsEntity)

    @Query("SELECT * FROM hidden_group WHERE mangaId = :mangaId")
    fun observeHidden(mangaId: Long): Flow<List<HiddenGroupEntity>>

    @Upsert
    suspend fun hide(group: HiddenGroupEntity)

    @Query("DELETE FROM hidden_group WHERE mangaId = :mangaId AND groupName = :group")
    suspend fun unhide(
        mangaId: Long,
        group: String,
    )
}
