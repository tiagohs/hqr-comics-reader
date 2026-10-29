package com.tiagohs.hqr.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/** The single database `library.db` (ADR-004). Each schema version is committed under schemas/. */
@Database(
    entities = [
        SourceEntity::class, MangaEntity::class, MangaReaderSettingsEntity::class, ChapterEntity::class,
        HiddenGroupEntity::class, ProgressEntity::class, HistoryEntity::class, CategoryEntity::class,
        MangaCategoryEntity::class, MergeEntity::class, MergeMemberEntity::class, DownloadEntity::class,
        SavedSearchEntity::class, FeedItemEntity::class, RecentSearchEntity::class, ServerEntity::class,
    ],
    version = HqrDatabase.VERSION,
    exportSchema = true,
)
@ConstructedBy(HqrDatabaseConstructor::class)
abstract class HqrDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao

    abstract fun chapterDao(): ChapterDao

    abstract fun progressDao(): ProgressDao

    abstract fun historyDao(): HistoryDao

    abstract fun categoryDao(): CategoryDao

    abstract fun downloadDao(): DownloadDao

    abstract fun sourceDao(): SourceDao

    abstract fun searchDao(): SearchDao

    abstract fun serverDao(): ServerDao

    abstract fun readerSettingsDao(): ReaderSettingsDao

    companion object {
        const val NAME = "library.db"
        const val VERSION = 1
    }
}

@Suppress("KotlinNoActualForExpect")
expect object HqrDatabaseConstructor : RoomDatabaseConstructor<HqrDatabase> {
    override fun initialize(): HqrDatabase
}
