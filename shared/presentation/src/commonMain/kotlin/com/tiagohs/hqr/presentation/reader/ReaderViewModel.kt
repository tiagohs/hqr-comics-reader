package com.tiagohs.hqr.presentation.reader

import androidx.lifecycle.SavedStateHandle
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

/** Loads page images into the cache ahead of the reader (RF-41). */
fun interface PagePrefetcher {
    suspend fun prefetch(
        manga: Manga,
        page: SourcePage,
    )
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
    private val saved: SavedStateHandle = SavedStateHandle(),
    private val prefetcher: PagePrefetcher? = null,
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
    private var endDismissed = false
    private val nextPages = HashMap<Long, List<SourcePage>>()
    private var prefetchJob: Job? = null
    private val prefetched = HashSet<String>()

    init {
        // RF-88: after the process dies, the chapter and page the reader was on win over the route's arguments.
        val chapter = saved.get<Long>(KEY_CHAPTER) ?: chapterId
        val page = saved.get<Int>(KEY_PAGE) ?: startPage
        viewModelScope.launch { open(chapter, page) }
    }

    private suspend fun open(
        id: Long,
        page: Int,
    ) {
        endDismissed = false
        _state.update { it.copy(loading = true, error = null, transition = null, pages = emptyList(), failedPages = emptySet()) }
        val manga = mangas.get(mangaId) ?: return
        val chapter = chapters.get(id) ?: return
        allChapters = chapters.list(mangaId)
        val settings = settingsStore.forManga(manga)
        attempt { nextPages.remove(chapter.id) ?: pageSource.pages(manga, chapter) }
            .onSuccess { pages ->
                val start =
                    (
                        if (page <
                            0
                        ) {
                            chapter.lastPage.takeIf { !chapter.read } ?: 0
                        } else {
                            page
                        }
                    ).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                // History and the opening page are written before the reader shows, so a fast first swipe is never
                // overwritten by them (RF-43).
                history.record(chapter.id, clock.now().toEpochMilliseconds())
                if (pages.isNotEmpty()) chapters.saveProgress(chapter.id, start, pages.size)
                saved[KEY_CHAPTER] = chapter.id
                saved[KEY_PAGE] = start
                _state.update {
                    it.copy(
                        manga = manga,
                        chapter = chapter,
                        pages = pages,
                        page = start,
                        loading = false,
                        settings = settings,
                        previous = ReadingRules.previousChapter(allChapters, chapter),
                        next = ReadingRules.nextChapter(allChapters, chapter),
                    )
                }
                prefetchAround(start)
            }.onFailure { e ->
                _state.update { it.copy(manga = manga, chapter = chapter, loading = false, error = e.toErrorKind(), settings = settings) }
            }
    }

    /** Called by the pager when the visible page changes. */
    fun onPageChanged(page: Int) {
        val current = _state.value
        if (page == current.page || current.pages.isEmpty()) return
        _state.update { it.copy(page = page) }
        saved[KEY_PAGE] = page
        if (page < current.pageCount - 1) endDismissed = false
        prefetchAround(page)
        save(page, immediate = false)
    }

    /** The reader reached the page after the last one: the transition (RF-42). */
    fun onReachedEnd() {
        val s = _state.value
        val chapter = s.chapter ?: return
        save(s.pageCount - 1, immediate = true)
        // After "back to reading", staying at the end does not reopen the transition.
        if (endDismissed) return
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

    fun dismissTransition() {
        endDismissed = true
        _state.update { it.copy(transition = null) }
    }

    fun toggleMenu() = _state.update { it.copy(menuVisible = !it.menuVisible) }

    fun hideMenu() = _state.update { it.copy(menuVisible = false) }

    /**
     * RF-41: the next [ReaderSettings.preload] pages go to the cache; 3 pages before the end, the next chapter's page
     * list is asked too, so turning to it starts at once.
     */
    private fun prefetchAround(page: Int) {
        val s = _state.value
        val manga = s.manga ?: return
        prefetchJob?.cancel()
        prefetchJob =
            viewModelScope.launch {
                val next = s.next
                if (next != null && page >= s.pageCount - NEXT_CHAPTER_DISTANCE && next.id !in nextPages) {
                    attempt { pageSource.pages(manga, next) }.onSuccess { nextPages[next.id] = it }
                }
                val ahead = s.pages.drop(page + 1).take(s.settings.preload)
                for (p in ahead) {
                    if (!prefetched.add(p.imageUrl)) continue
                    attempt { prefetcher?.prefetch(manga, p) }.onFailure { prefetched.remove(p.imageUrl) }
                }
            }
    }

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
        private const val NEXT_CHAPTER_DISTANCE = 3
        private const val KEY_CHAPTER = "reader.chapter"
        private const val KEY_PAGE = "reader.page"
    }
}
