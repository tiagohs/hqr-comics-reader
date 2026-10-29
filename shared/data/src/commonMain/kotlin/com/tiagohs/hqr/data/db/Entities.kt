package com.tiagohs.hqr.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Data model of architecture §5. Lists and maps are stored as JSON text.

@Entity(tableName = "source")
data class SourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val lang: String,
    val schemaVersion: Int,
    val enabled: Boolean,
    val settings: String,
)

@Entity(
    tableName = "manga",
    indices = [Index(value = ["sourceId", "key"], unique = true), Index("inLibrary")],
)
data class MangaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val key: String,
    val url: String?,
    val title: String,
    val alternativeTitles: String,
    val coverUrl: String?,
    val authors: String,
    val artists: String,
    val status: String,
    val genres: String,
    val language: String?,
    val description: String?,
    val kind: String,
    val inLibrary: Boolean,
    val addedAt: Long?,
    val coverColor: Int?,
    val updatedAt: Long,
)

@Entity(
    tableName = "manga_reader_settings",
    foreignKeys = [ForeignKey(MangaEntity::class, ["id"], ["mangaId"], onDelete = ForeignKey.CASCADE)],
)
data class MangaReaderSettingsEntity(
    @PrimaryKey val mangaId: Long,
    val mode: String?,
    val scale: String?,
    val tapZones: String?,
    val background: String?,
    val doublePage: Boolean?,
    val cropBorders: Boolean?,
)

@Entity(
    tableName = "chapter",
    indices = [Index(value = ["mangaId", "key"], unique = true), Index("mangaId")],
    foreignKeys = [ForeignKey(MangaEntity::class, ["id"], ["mangaId"], onDelete = ForeignKey.CASCADE)],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Long,
    val key: String,
    val number: Double,
    val title: String?,
    val volume: String?,
    val language: String?,
    @ColumnInfo(name = "groupName") val group: String?,
    val groupUrl: String?,
    val uploadedAt: Long?,
    val fetchedAt: Long,
    val removedAtSource: Boolean,
    val externalUrl: String?,
    val sourceOrder: Int,
)

@Entity(
    tableName = "hidden_group",
    primaryKeys = ["mangaId", "groupName"],
    foreignKeys = [ForeignKey(MangaEntity::class, ["id"], ["mangaId"], onDelete = ForeignKey.CASCADE)],
)
data class HiddenGroupEntity(
    val mangaId: Long,
    val groupName: String,
)

@Entity(
    tableName = "progress",
    foreignKeys = [ForeignKey(ChapterEntity::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
)
data class ProgressEntity(
    @PrimaryKey val chapterId: Long,
    val page: Int,
    val pageCount: Int?,
    val read: Boolean,
    val updatedAt: Long,
)

@Entity(
    tableName = "history",
    indices = [Index("readAt")],
    foreignKeys = [ForeignKey(ChapterEntity::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
)
data class HistoryEntity(
    @PrimaryKey val chapterId: Long,
    val readAt: Long,
)

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "manga_category",
    primaryKeys = ["mangaId", "categoryId"],
    indices = [Index("categoryId")],
    foreignKeys = [
        ForeignKey(MangaEntity::class, ["id"], ["mangaId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class MangaCategoryEntity(
    val mangaId: Long,
    val categoryId: Long,
)

@Entity(tableName = "merge")
data class MergeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
)

@Entity(
    tableName = "merge_member",
    primaryKeys = ["mergeId", "mangaId"],
    indices = [Index("mangaId")],
    foreignKeys = [
        ForeignKey(MergeEntity::class, ["id"], ["mergeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MangaEntity::class, ["id"], ["mangaId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class MergeMemberEntity(
    val mergeId: Long,
    val mangaId: Long,
    val priority: Int,
)

@Entity(
    tableName = "download",
    foreignKeys = [ForeignKey(ChapterEntity::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
)
data class DownloadEntity(
    @PrimaryKey val chapterId: Long,
    val state: String,
    val path: String?,
    val format: String,
    val queueOrder: Int,
    val pagesDone: Int,
    val pagesTotal: Int,
    val error: String?,
)

@Entity(tableName = "saved_search")
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val name: String,
    val query: String,
    val filters: String,
)

@Entity(tableName = "feed_item")
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val savedSearchId: Long?,
    val sortOrder: Int,
)

@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Entity(tableName = "server")
data class ServerEntity(
    @PrimaryKey val sourceId: String,
    val kind: String,
    val name: String,
    val url: String,
    val username: String?,
    val pinnedCertificate: String?,
)
