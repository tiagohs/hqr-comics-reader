package com.tiagohs.hqr.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadingRules
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** Where the pages come from: the downloaded files or the source (RF-65). */
fun interface ChapterPages {
    suspend fun pages(
        manga: Manga,
        chapter: Chapter,
    ): List<SourcePage>
}

/** Per-work and global reader settings (RF-37). */
interface ReaderSettingsStore {
    suspend fun forManga(manga: Manga): ReaderSettings

    suspend fun saveForManga(
        mangaId: Long,
        settings: ReaderSettings,
    )

    suspend fun saveGlobal(settings: ReaderSettings)
}

/**
 * The reader engine (ADR-007): pages, current page, saving progress 500 ms after the last change (RF-43), the
 * chapter transition with gap warning (RF-42) and the next chapter loaded near the end (RF-41).
 */
class ReaderViewModel(
    private val mangaId: Long,
    private val chapterId: Long,
    private val startPage: Int,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
    private val history: HistoryRepository,
    private val settingsStore: ReaderSettingsStore,
    private val pageSource: ChapterPages,
    private val clock: Clock,
) : ViewModel() {
    data class Transition(
        val finished: Chapter,
        val next: Chapter?,
        val missing: List<Int>,
    )

    data class State(
        val manga: Manga? = null,
        val chapter: Chapter? = null,
        val pages: List<SourcePage> = emptyList(),
        val page: Int = 0,
        val loading: Boolean = true,
        val error: ErrorKind? = null,
        val menuVisible: Boolean = false,
        val settings: ReaderSettings = ReaderSettings(),
        val previous: Chapter? = null,
        val next: Chapter? = null,
        val transition: Transition? = null,
        val failedPages: Set<Int> = emptySet(),
    ) {
        val pageCount: Int get() = pages.size
        val isEmptyChapter: Boolean get() = !loading && error == null && pages.isEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var saveJob: Job? = null
    private var allChapters: List<Chapter> = emptyList()

    init {
        viewModelScope.launch { open(chapterId, startPage) }
    }

    private suspend fun open(
        id: Long,
        page: Int,
    ) {
        _state.update { it.copy(loading = true, error = null, transition = null, pages = emptyList(), failedPages = emptySet()) }
        val manga = mangas.get(mangaId) ?: return
        val chapter = chapters.get(id) ?: return
        allChapters = chapters.list(mangaId)
        val settings = settingsStore.forManga(manga)
        attempt { pageSource.pages(manga, chapter) }
            .onSuccess { pages ->
                val start = if (page < 0) chapter.lastPage.takeIf { !chapter.read } ?: 0 else page
                _state.update {
                    it.copy(
                        manga = manga,
                        chapter = chapter,
                        pages = pages,
                        page = start.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
                        loading = false,
                        settings = settings,
                        previous = ReadingRules.previousChapter(allChapters, chapter),
                        next = ReadingRules.nextChapter(allChapters, chapter),
                    )
                }
                history.record(chapter.id, clock.now().toEpochMilliseconds())
                if (pages.isNotEmpty()) save(start.coerceIn(0, pages.size - 1), immediate = true)
            }.onFailure { e ->
                _state.update { it.copy(manga = manga, chapter = chapter, loading = false, error = e.toErrorKind(), settings = settings) }
            }
    }

    /** Called by the pager when the visible page changes. */
    fun onPageChanged(page: Int) {
        val current = _state.value
        if (page == current.page || current.pages.isEmpty()) return
        _state.update { it.copy(page = page) }
        save(page, immediate = false)
    }

    /** The reader reached the page after the last one: the transition (RF-42). */
    fun onReachedEnd() {
        val s = _state.value
        val chapter = s.chapter ?: return
        save(s.pageCount - 1, immediate = true)
        val next = s.next
        _state.update {
            it.copy(
                transition =
                    Transition(
                        chapter,
                        next,
                        next
                            ?.let { n ->
                                ReadingRules.missingBetween(chapter.number, n.number)
                            }.orEmpty(),
                    ),
            )
        }
    }

    fun dismissTransition() = _state.update { it.copy(transition = null) }

    fun toggleMenu() = _state.update { it.copy(menuVisible = !it.menuVisible) }

    fun hideMenu() = _state.update { it.copy(menuVisible = false) }

    fun goToNext() {
        val next = _state.value.next ?: return
        viewModelScope.launch { open(next.id, 0) }
    }

    fun goToPrevious() {
        val previous = _state.value.previous ?: return
        viewModelScope.launch { open(previous.id, 0) }
    }

    fun retry() {
        val id = _state.value.chapter?.id ?: chapterId
        viewModelScope.launch { open(id, _state.value.page) }
    }

    fun onPageFailed(page: Int) = _state.update { it.copy(failedPages = it.failedPages + page) }

    fun onPageRetry(page: Int) = _state.update { it.copy(failedPages = it.failedPages - page) }

    fun updateSettings(
        settings: ReaderSettings,
        onlyThisWork: Boolean,
    ) {
        _state.update { it.copy(settings = settings) }
        viewModelScope.launch {
            if (onlyThisWork) settingsStore.saveForManga(mangaId, settings) else settingsStore.saveGlobal(settings)
        }
    }

    private fun save(
        page: Int,
        immediate: Boolean,
    ) {
        val chapter = _state.value.chapter ?: return
        val count = _state.value.pageCount
        saveJob?.cancel()
        saveJob =
            viewModelScope.launch {
                if (!immediate) delay(SAVE_DELAY_MS)
                // A newer page cancels the wait, never a write already started.
                withContext(NonCancellable) { chapters.saveProgress(chapter.id, page, count) }
            }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Leaving the reader saves at once (RF-43).
        val s = _state.value
        val chapter = s.chapter ?: return
        if (s.pageCount > 0) {
            kotlinx.coroutines.GlobalScope.launch { chapters.saveProgress(chapter.id, s.page, s.pageCount) }
        }
    }

    companion object {
        const val SAVE_DELAY_MS = 500L
    }
}
