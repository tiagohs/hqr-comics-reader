package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.attempt

/** How far an update of the library went (RF-58). */
data class UpdateProgress(
    val done: Int,
    val total: Int,
    val current: String? = null,
    val newChapters: Int = 0,
    val failed: Int = 0,
)

/**
 * RF-58: new chapters for the library's online works, one work after another (so each source keeps its own pace,
 * RF-04). Every work's chapters are saved as they arrive: stopping keeps what already came.
 */
class LibraryUpdater(
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
) {
    suspend fun run(
        works: List<Manga>,
        progress: (UpdateProgress) -> Unit,
    ): UpdateProgress {
        registry.awaitReady()
        // Local files are read again when opened (RF-15); a source the app doesn't have can't be asked.
        val online = works.filter { it.sourceId != SourceId.LOCAL && registry[it.sourceId] != null }.sortedBy { it.sourceId.value }
        var state = UpdateProgress(0, online.size)
        progress(state)
        for (manga in online) {
            state = state.copy(current = manga.title)
            progress(state)
            val before = chapters.list(manga.id).map { it.id }.toSet()
            val result =
                attempt {
                    val source = registry.awaitRequire(manga.sourceId)
                    val details = source.details(manga.source)
                    mangas.upsert(details)
                    chapters.sync(manga.id, source.chapters(details))
                }
            val arrived = result.getOrNull()?.count { it.id !in before && !it.removedAtSource } ?: 0
            state =
                state.copy(
                    done = state.done + 1,
                    newChapters = state.newChapters + arrived,
                    failed = state.failed + if (result.isFailure) 1 else 0,
                )
            progress(state)
        }
        return state.copy(current = null).also(progress)
    }
}
