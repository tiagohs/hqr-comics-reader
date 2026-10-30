package com.tiagohs.hqr.domain.repository

import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import kotlinx.coroutines.flow.Flow

/** Works saved on the device (library, seen works) keyed by (sourceId, key) (RF-90). */
interface MangaRepository {
    /** Saves or refreshes what the source said about the work and returns the local copy. */
    suspend fun upsert(source: SourceManga): Manga

    suspend fun get(id: Long): Manga?

    suspend fun find(
        sourceId: SourceId,
        key: String,
    ): Manga?

    fun observe(id: Long): Flow<Manga?>

    fun observeLibrary(): Flow<List<LibraryItem>>

    suspend fun setInLibrary(
        id: Long,
        inLibrary: Boolean,
    )

    suspend fun setCoverColor(
        id: Long,
        color: Int?,
    )

    /** Records that the work screen opened now; returns the previous opening, or null the first time (RF-33). */
    suspend fun markOpened(id: Long): Long?

    /** Keys already in the library for one source: the "Na biblioteca" badge (RF-26). */
    fun observeLibraryKeys(sourceId: SourceId): Flow<Set<String>>
}

/** One work in the library grid (RF-54) with its counters. */
data class LibraryItem(
    val manga: Manga,
    val unread: Int,
    val downloaded: Int,
    val categoryIds: Set<Long>,
    val lastReadAtEpochMs: Long?,
    val latestChapterAtEpochMs: Long?,
    val totalChapters: Int,
)

interface ChapterRepository {
    /**
     * Replaces the chapter list with what the source returned (RF-33): progress and downloads are kept; chapters
     * that disappeared are kept and marked "Removido da fonte" (RF-92).
     */
    suspend fun sync(
        mangaId: Long,
        chapters: List<SourceChapter>,
    ): List<Chapter>

    fun observe(mangaId: Long): Flow<List<Chapter>>

    suspend fun list(mangaId: Long): List<Chapter>

    suspend fun get(id: Long): Chapter?

    /** RF-43: the page is saved after each change; the last page marks the chapter as read. */
    suspend fun saveProgress(
        chapterId: Long,
        page: Int,
        pageCount: Int,
    )

    /** RF-32: one, several or all before a chapter. */
    suspend fun setRead(
        chapterIds: Collection<Long>,
        read: Boolean,
    )

    suspend fun setDownloaded(
        chapterId: Long,
        downloaded: Boolean,
    )
}

interface CategoryRepository {
    fun observe(): Flow<List<Category>>

    suspend fun create(name: String): Category

    suspend fun rename(
        id: Long,
        name: String,
    )

    suspend fun reorder(idsInOrder: List<Long>)

    /** RF-53: works of a deleted category go to "Sem categoria". */
    suspend fun delete(id: Long)

    suspend fun setCategories(
        mangaId: Long,
        categoryIds: Set<Long>,
    )
}

/** One reading in the history (RF-59). */
data class HistoryEntry(
    val manga: Manga,
    val chapter: Chapter,
    val readAtEpochMs: Long,
)

interface HistoryRepository {
    fun observe(): Flow<List<HistoryEntry>>

    suspend fun record(
        chapterId: Long,
        atEpochMs: Long,
    )

    suspend fun remove(chapterId: Long)

    suspend fun clear()

    /** The last chapter read of each work, for "Continuar lendo" in the library. */
    fun observeLatestPerManga(): Flow<List<HistoryEntry>>
}
