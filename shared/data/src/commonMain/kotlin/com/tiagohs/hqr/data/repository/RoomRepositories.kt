package com.tiagohs.hqr.data.repository

import androidx.room3.withWriteTransaction
import com.tiagohs.hqr.data.db.CategoryEntity
import com.tiagohs.hqr.data.db.ChapterEntity
import com.tiagohs.hqr.data.db.HistoryEntity
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.MangaCategoryEntity
import com.tiagohs.hqr.data.db.ProgressEntity
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class RoomMangaRepository(
    private val db: HqrDatabase,
    private val clock: Clock,
) : MangaRepository {
    private val dao = db.mangaDao()

    /** Find and write in one transaction: two screens saving the same work at once never collide on the key. */
    override suspend fun upsert(source: SourceManga): Manga = db.withWriteTransaction { upsertUnlocked(source) }

    private suspend fun upsertUnlocked(source: SourceManga): Manga {
        val now = clock.now().toEpochMilliseconds()
        val existing = dao.find(source.sourceId.value, source.key)
        if (existing == null) {
            val id = dao.insert(source.toEntity(now))
            return requireNotNull(dao.get(id)).toDomain()
        }
        // A listing brings less than the details: never erase what the details already saved.
        val merged = source.mergeOver(existing.toSource())
        dao.updateFromSource(
            id = existing.id,
            url = merged.url,
            title = merged.title,
            alternativeTitles = merged.alternativeTitles.toJson(),
            coverUrl = merged.coverUrl,
            authors = merged.authors.toJson(),
            artists = merged.artists.toJson(),
            status = merged.status.name,
            genres = merged.genres.toJson(),
            language = merged.language,
            description = merged.description,
            kind = merged.kind.name,
            updatedAt = now,
        )
        // RF-98: a new cover means a new color, computed again on the next opening.
        if (merged.coverUrl != existing.coverUrl) dao.setCoverColor(existing.id, null)
        return requireNotNull(dao.get(existing.id)).toDomain()
    }

    override suspend fun get(id: Long): Manga? = dao.get(id)?.toDomain()

    override suspend fun find(
        sourceId: SourceId,
        key: String,
    ): Manga? = dao.find(sourceId.value, key)?.toDomain()

    override fun observe(id: Long): Flow<Manga?> = dao.observe(id).map { it?.toDomain() }

    override fun observeLibrary(): Flow<List<LibraryItem>> =
        combine(dao.observeLibrary(), dao.observeLibraryRows(), db.categoryDao().observeAssignments()) { mangas, rows, links ->
            val byId = rows.associateBy { it.id }
            val categories = links.groupBy({ it.mangaId }, { it.categoryId })
            mangas.map { entity ->
                val row = byId[entity.id]
                LibraryItem(
                    manga = entity.toDomain(),
                    unread = row?.unread ?: 0,
                    downloaded = row?.downloaded ?: 0,
                    categoryIds = categories[entity.id]?.toSet() ?: emptySet(),
                    lastReadAtEpochMs = row?.lastReadAt,
                    latestChapterAtEpochMs = row?.latestUploadAt,
                    totalChapters = row?.total ?: 0,
                )
            }
        }

    override suspend fun setInLibrary(
        id: Long,
        inLibrary: Boolean,
    ) {
        val current = dao.get(id) ?: return
        val addedAt = if (inLibrary) current.addedAt ?: clock.now().toEpochMilliseconds() else null
        dao.setInLibrary(id, inLibrary, addedAt)
    }

    override suspend fun markOpened(id: Long): Long? =
        db.withWriteTransaction {
            val previous = dao.get(id)?.lastOpenedAt
            dao.setOpened(id, clock.now().toEpochMilliseconds())
            previous
        }

    override suspend fun setCoverColor(
        id: Long,
        color: Int?,
    ) = dao.setCoverColor(id, color)

    override fun observeLibraryKeys(sourceId: SourceId): Flow<Set<String>> = dao.observeLibraryKeys(sourceId.value).map { it.toSet() }
}

private fun SourceManga.mergeOver(old: SourceManga): SourceManga =
    copy(
        coverUrl = coverUrl ?: old.coverUrl,
        url = url ?: old.url,
        alternativeTitles = alternativeTitles.ifEmpty { old.alternativeTitles },
        authors = authors.ifEmpty { old.authors },
        artists = artists.ifEmpty { old.artists },
        status = if (status == com.tiagohs.hqr.domain.model.PublicationStatus.UNKNOWN) old.status else status,
        genres = genres.ifEmpty { old.genres },
        description = description ?: old.description,
        language = language ?: old.language,
        kind = if (kind == com.tiagohs.hqr.domain.model.ReadingKind.UNKNOWN) old.kind else kind,
    )

class RoomChapterRepository(
    private val db: HqrDatabase,
    private val clock: Clock,
) : ChapterRepository {
    private val chapters = db.chapterDao()
    private val progress = db.progressDao()
    private val downloads = db.downloadDao()

    override suspend fun sync(
        mangaId: Long,
        chapters: List<SourceChapter>,
    ): List<Chapter> {
        val now = clock.now().toEpochMilliseconds()
        db.withWriteTransaction {
            val existing = this@RoomChapterRepository.chapters.list(mangaId).associateBy { it.key }
            val incomingKeys = chapters.map { it.key }.toSet()
            chapters.forEachIndexed { index, chapter ->
                val old = existing[chapter.key]
                val entity =
                    ChapterEntity(
                        id = old?.id ?: 0,
                        mangaId = mangaId,
                        key = chapter.key,
                        number = chapter.number,
                        title = chapter.title,
                        volume = chapter.volume,
                        language = chapter.language,
                        group = chapter.group,
                        groupUrl = chapter.groupUrl,
                        uploadedAt = chapter.uploadedAtEpochMs,
                        fetchedAt = old?.fetchedAt ?: now,
                        removedAtSource = false,
                        externalUrl = chapter.externalUrl,
                        sourceOrder = index,
                    )
                if (old == null) this@RoomChapterRepository.chapters.insert(entity) else this@RoomChapterRepository.chapters.upsert(entity)
            }
            // RF-92: what vanished from the source stays, with its progress and files, marked as removed.
            existing.values
                .filter { it.key !in incomingKeys && !it.removedAtSource }
                .forEach { this@RoomChapterRepository.chapters.setRemoved(it.id, true) }
        }
        return list(mangaId)
    }

    override fun observe(mangaId: Long): Flow<List<Chapter>> =
        combine(chapters.observe(mangaId), progress.observeForManga(mangaId), downloads.observe()) { list, prog, dl ->
            val byChapter = prog.associateBy { it.chapterId }
            val done = dl.filter { it.state == DOWNLOAD_DONE }.map { it.chapterId }.toSet()
            list.map { it.toDomain(byChapter[it.id], it.id in done) }
        }

    override suspend fun list(mangaId: Long): List<Chapter> {
        val byChapter = progress.listForManga(mangaId).associateBy { it.chapterId }
        val done =
            downloads
                .list()
                .filter { it.state == DOWNLOAD_DONE }
                .map { it.chapterId }
                .toSet()
        return chapters.list(mangaId).map { it.toDomain(byChapter[it.id], it.id in done) }
    }

    override suspend fun get(id: Long): Chapter? {
        val entity = chapters.get(id) ?: return null
        return entity.toDomain(progress.get(id), downloads.get(id)?.state == DOWNLOAD_DONE)
    }

    override suspend fun saveProgress(
        chapterId: Long,
        page: Int,
        pageCount: Int,
    ) {
        val old = progress.get(chapterId)
        val reachedEnd = pageCount > 0 && page >= pageCount - 1
        progress.upsert(
            ProgressEntity(
                chapterId = chapterId,
                page = page,
                pageCount = pageCount,
                read = (old?.read ?: false) || reachedEnd,
                updatedAt = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    override suspend fun setRead(
        chapterIds: Collection<Long>,
        read: Boolean,
    ) {
        val now = clock.now().toEpochMilliseconds()
        db.withWriteTransaction {
            chapterIds.forEach { id ->
                val old = progress.get(id)
                progress.upsert(
                    ProgressEntity(
                        chapterId = id,
                        page = if (read) old?.page ?: 0 else 0,
                        pageCount = old?.pageCount,
                        read = read,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    override suspend fun setDownloaded(
        chapterId: Long,
        downloaded: Boolean,
    ) {
        // The download queue owns the row; this only exists for sources whose files are already local.
        if (!downloaded) downloads.delete(chapterId)
    }

    companion object {
        const val DOWNLOAD_DONE = "DONE"
    }
}

class RoomCategoryRepository(
    private val db: HqrDatabase,
) : CategoryRepository {
    private val dao = db.categoryDao()

    override fun observe(): Flow<List<Category>> = dao.observe().map { list -> list.map { Category(it.id, it.name, it.sortOrder) } }

    override suspend fun create(name: String): Category {
        val order = (dao.list().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = dao.insert(CategoryEntity(name = name.trim(), sortOrder = order))
        return Category(id, name.trim(), order)
    }

    override suspend fun rename(
        id: Long,
        name: String,
    ) = dao.rename(id, name.trim())

    override suspend fun reorder(idsInOrder: List<Long>) =
        db.withWriteTransaction {
            idsInOrder.forEachIndexed { index, id -> dao.setOrder(id, index + 1) }
        }

    // RF-53: the works only lose this category; with none left they show under "Sem categoria".
    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setCategories(
        mangaId: Long,
        categoryIds: Set<Long>,
    ) = db.withWriteTransaction {
        dao.clearForManga(mangaId)
        categoryIds.forEach { dao.assign(MangaCategoryEntity(mangaId, it)) }
    }
}

class RoomHistoryRepository(
    private val db: HqrDatabase,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
) : HistoryRepository {
    private val dao = db.historyDao()

    override fun observe(): Flow<List<HistoryEntry>> =
        dao.observe().map { rows ->
            rows.mapNotNull {
                load(it.mangaId, it.chapterId, it.readAt)
            }
        }

    override fun observeLatestPerManga(): Flow<List<HistoryEntry>> =
        dao.observeLatestPerManga().map { rows -> rows.mapNotNull { load(it.mangaId, it.chapterId, it.readAt) } }

    private suspend fun load(
        mangaId: Long,
        chapterId: Long,
        readAt: Long,
    ): HistoryEntry? {
        val manga = mangas.get(mangaId) ?: return null
        val chapter = chapters.get(chapterId) ?: return null
        return HistoryEntry(manga, chapter, readAt)
    }

    override suspend fun record(
        chapterId: Long,
        atEpochMs: Long,
    ) = dao.upsert(HistoryEntity(chapterId, atEpochMs))

    override suspend fun remove(chapterId: Long) = dao.delete(chapterId)

    override suspend fun clear() = dao.clear()
}
