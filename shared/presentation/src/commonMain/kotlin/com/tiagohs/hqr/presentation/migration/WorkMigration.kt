package com.tiagohs.hqr.presentation.migration

import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import kotlinx.coroutines.flow.first

/**
 * RF-69: a work moved to another source. The new one takes the categories, the chapters read (matched by number) and
 * the history, and takes the old one's place in the library; the old downloads go only when asked.
 */
class WorkMigration(
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
    private val history: HistoryRepository,
    private val categories: CategoryRepository? = null,
    private val downloads: Downloads? = null,
) {
    /** How it went: the new work and how many read chapters found their match. */
    data class Result(
        val mangaId: Long,
        val readCarried: Int,
        val readMissing: Int,
    )

    suspend fun migrate(
        fromId: Long,
        target: SourceManga,
        deleteDownloads: Boolean,
    ): Result {
        val old = mangas.get(fromId) ?: throw SourceException("Obra apagada", SourceException.Kind.NOT_FOUND)
        // Everything from the new source first: a source that fails leaves the old work untouched.
        val source = registry.awaitRequire(target.sourceId)
        val details = source.details(target)
        val found = source.chapters(details)
        val new = mangas.upsert(details)
        if (new.id == fromId) return Result(fromId, 0, 0)
        chapters.sync(new.id, found)
        val newChapters = chapters.list(new.id)
        val oldChapters = chapters.list(fromId)
        val byNumber = newChapters.groupBy { it.number }
        val readNumbers = oldChapters.filter { it.read }.map { it.number }.toSet()
        val toRead = readNumbers.flatMap { byNumber[it].orEmpty() }.map { it.id }
        chapters.setRead(toRead, read = true)
        // The history keeps when each chapter was read.
        history
            .observe()
            .first()
            .filter { it.manga.id == fromId }
            .forEach { entry ->
                byNumber[entry.chapter.number]?.firstOrNull()?.let { history.record(it.id, entry.readAtEpochMs) }
                // Moved, not copied: the history shows the work once.
                history.remove(entry.chapter.id)
            }
        val oldCategories = categories?.observeFor(fromId)?.first().orEmpty()
        if (old.inLibrary) mangas.setInLibrary(new.id, true)
        categories?.setCategories(new.id, oldCategories)
        if (old.inLibrary) mangas.removeFromLibrary(fromId)
        // Every old chapter: the queue drops what it has of each, downloaded or still waiting.
        if (deleteDownloads) downloads?.remove(oldChapters.map { it.id })
        val carried = readNumbers.count { it in byNumber }
        return Result(new.id, carried, readNumbers.size - carried)
    }
}
