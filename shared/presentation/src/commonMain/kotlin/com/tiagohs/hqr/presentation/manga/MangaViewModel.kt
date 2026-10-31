package com.tiagohs.hqr.presentation.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.ReadingRules
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.CacheStore
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** The work screen (SCR-OBR-001): details, chapters, the main button and the chapter actions (RF-29 to RF-33). */
@Suppress("TooManyFunctions") // One screen, many chapter actions: read, select, download, categories.
class MangaViewModel(
    private val mangaId: Long,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
    private val coverPixels: CoverPixels? = null,
    private val categories: CategoryRepository? = null,
    private val downloads: Downloads? = null,
    private val connectivity: Connectivity = Connectivity.Always,
    private val cache: CacheStore = CacheStore.None,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    enum class Order { NEWEST_FIRST, OLDEST_FIRST }

    enum class ChapterFilter { ALL, UNREAD, DOWNLOADED }

    data class State(
        val manga: Manga? = null,
        val chapters: List<Chapter> = emptyList(),
        val action: ReadAction = ReadAction.Nothing,
        val refreshing: Boolean = true,
        val error: ErrorKind? = null,
        /** RF-117: the chapter list still on its way; the rest of the screen is already usable. */
        val chaptersLoading: Boolean = true,
        /** RF-117: the chapter list failed; details and what was saved still show. */
        val chaptersError: ErrorKind? = null,
        val order: Order = Order.NEWEST_FIRST,
        val filter: ChapterFilter = ChapterFilter.ALL,
        val totalChapters: Int = 0,
        val group: String? = null,
        val groups: List<String> = emptyList(),
        val newChapterIds: Set<Long> = emptySet(),
    )

    /** RF-53: every category and the ones this work is in. */
    val categoryChoice: StateFlow<Pair<List<Category>, Set<Long>>> =
        (categories?.let { combine(it.observe(), it.observeFor(mangaId), ::Pair) } ?: flowOf(emptyList<Category>() to emptySet()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList<Category>() to emptySet())

    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<ErrorKind?>(null)
    private val chaptersLoading = MutableStateFlow(true)
    private val chaptersError = MutableStateFlow<ErrorKind?>(null)
    private val order = MutableStateFlow(Order.NEWEST_FIRST)
    private val filter = MutableStateFlow(ChapterFilter.ALL)
    private val group = MutableStateFlow<String?>(null)

    // The previous opening of this work; chapters seen after it are "new" until the next opening (RF-33).
    private val previousOpen = MutableStateFlow<Long?>(null)

    val state: StateFlow<State> =
        combine(
            mangas.observe(mangaId),
            chapters.observe(mangaId),
            combine(refreshing, error, previousOpen, ::Triple),
            combine(chaptersLoading, chaptersError, order, ::Triple),
            combine(filter, group, ::Pair),
        ) { manga, list, (busy, err, opened), (chLoading, chError, ord), (fil, grp) ->
            val visible =
                list
                    .filter {
                        when (fil) {
                            ChapterFilter.ALL -> true
                            ChapterFilter.UNREAD -> !it.read
                            ChapterFilter.DOWNLOADED -> it.downloaded
                        }
                    }.filter { grp == null || it.source.group == grp }
                    .let { if (ord == Order.NEWEST_FIRST) it.sortedByDescending(Chapter::number) else it.sortedBy(Chapter::number) }
            State(
                manga = manga,
                chapters = visible,
                action = ReadingRules.readAction(list),
                refreshing = busy,
                error = err,
                // Saved chapters show at once; the skeleton only covers an empty list (RF-117).
                chaptersLoading = chLoading && list.isEmpty(),
                chaptersError = chError,
                order = ord,
                filter = fil,
                totalChapters = list.map { it.number }.distinct().size,
                group = grp,
                groups = list.mapNotNull { it.source.group }.distinct().sorted(),
                newChapterIds = if (opened == null) emptySet() else list.filter { it.fetchedAtEpochMs > opened }.map { it.id }.toSet(),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State())

    init {
        refresh(markOpened = true, force = false)
    }

    /** RF-98, ADR-021: the seed is computed once per cover and saved with the work. */
    private suspend fun colorFromCover() {
        val manga = mangas.get(mangaId) ?: return
        val reader = coverPixels
        if (manga.coverColor != null || manga.source.coverUrl == null || reader == null) return
        val pixels = attempt { reader.pixels(manga) }.getOrNull() ?: return
        // The cover may have changed while it was being read: never save the color of an old cover.
        if (mangas.get(mangaId)?.source?.coverUrl == manga.source.coverUrl) mangas.setCoverColor(mangaId, CoverColor.seed(pixels))
    }

    /** RF-33: rereads details and chapters; on failure keeps what was there and warns. */
    fun refresh() = refresh(markOpened = false, force = true)

    /**
     * RF-117: details and chapters are asked at the same time, each one showing as it arrives. Opening a work refreshed
     * less than [FRESH_MS] ago asks nothing; pulling always asks. The opening is recorded after the first refresh, so
     * the chapters it brings are never "new" next time.
     */
    private fun refresh(
        markOpened: Boolean,
        force: Boolean,
    ) {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            chaptersError.value = null
            val manga = mangas.get(mangaId)
            if (manga == null) {
                error.value = ErrorKind.NOT_FOUND
                finish(markOpened)
                return@launch
            }
            val saved = chapters.list(mangaId).isNotEmpty()
            val fresh = saved && cache.read(freshKey)?.let { clock.now().toEpochMilliseconds() - it.savedAtEpochMs < FRESH_MS } == true
            // RF-87: offline, pulling to refresh changes nothing (files on the device still refresh).
            if (!connectivity.online.value && manga.sourceId != SourceId.LOCAL) {
                if (force || !saved) error.value = ErrorKind.OFFLINE
                finish(markOpened)
                return@launch
            }
            if (fresh && !force) {
                finish(markOpened)
                return@launch
            }
            val source = attempt { registry.awaitRequire(manga.sourceId) }
            val found = source.getOrNull()
            if (found == null) {
                val kind = source.exceptionOrNull()?.toErrorKind() ?: ErrorKind.UNKNOWN
                error.value = kind
                chaptersError.value = kind
                finish(markOpened)
                return@launch
            }
            coroutineScope {
                launch {
                    attempt { mangas.upsert(found.details(manga.source)) }.onFailure { error.value = it.toErrorKind() }
                }
                launch {
                    attempt { chapters.sync(mangaId, found.chapters(manga.source)) }
                        .onSuccess { cache.write(freshKey, "") }
                        .onFailure { chaptersError.value = it.toErrorKind() }
                    chaptersLoading.value = false
                }
            }
            finish(markOpened)
        }
    }

    private suspend fun finish(markOpened: Boolean) {
        chaptersLoading.value = false
        if (markOpened) previousOpen.value = mangas.markOpened(mangaId)
        refreshing.value = false
        // After the refresh: a cover changed by it is the one that gives the color.
        if (markOpened) colorFromCover()
    }

    private val freshKey = "manga.$mangaId.refreshed"

    fun setCategories(ids: Set<Long>) {
        viewModelScope.launch { categories?.setCategories(mangaId, ids) }
    }

    // RF-52: a removal waiting for "Desfazer"; a flow so the snackbar survives the screen being recreated.
    private val _removed = kotlinx.coroutines.channels.Channel<LibraryRemoval>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val removed: kotlinx.coroutines.flow.Flow<LibraryRemoval> = _removed.receiveAsFlow()

    /** RF-52: puts the work in or takes it out; a removal comes out of [removed] for "Desfazer". */
    fun toggleLibrary() {
        viewModelScope.launch {
            val manga = mangas.get(mangaId) ?: return@launch
            if (manga.inLibrary) {
                mangas.removeFromLibrary(mangaId)?.let { _removed.send(it) }
            } else {
                mangas.setInLibrary(mangaId, true)
            }
        }
    }

    /** "Desfazer": back in the library with the same date added and categories. */
    fun undoRemove(removal: LibraryRemoval) {
        viewModelScope.launch { mangas.restoreToLibrary(removal) }
    }

    fun setOrder(value: Order) {
        order.value = value
    }

    fun setFilter(value: ChapterFilter) {
        filter.value = value
    }

    /** RF-30: only one scan group's chapters; null shows all. */
    fun setGroup(value: String?) {
        group.value = value
    }

    /** RF-32: one or several chapters. */
    fun setRead(
        ids: Collection<Long>,
        read: Boolean,
    ) {
        viewModelScope.launch {
            chapters.setRead(ids, read)
            if (read) downloads?.chaptersRead(ids)
        }
        clearSelection()
    }

    /** Downloads exist on this platform and the work is not already on the device. */
    val canDownload: Boolean get() = downloads != null && state.value.manga?.sourceId != SourceId.LOCAL

    /** The queue and downloaded state of this work's chapters, by chapter id (SCR-OBR-001). */
    val downloadStates: StateFlow<Map<Long, DownloadItem>> =
        (downloads?.observe() ?: flowOf(emptyList()))
            .map { items -> items.filter { it.mangaId == mangaId }.associateBy { it.chapterId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyMap())

    /** Chapters picked with a long press: download or mark them together (RF-32, RF-62). */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    fun toggleSelected(id: Long) {
        _selected.value = _selected.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    /** RF-62: one chapter or the selected ones. */
    fun download(ids: Collection<Long>) {
        val queue = downloads ?: return
        viewModelScope.launch {
            val order = chapters.list(mangaId).sortedBy { it.number }.filter { it.id in ids && it.source.externalUrl == null }
            queue.enqueue(order.map { it.id })
        }
        clearSelection()
    }

    /** RF-62: the next [count] unread chapters, or all of them (null). */
    fun downloadUnread(count: Int? = null) {
        val queue = downloads ?: return
        viewModelScope.launch {
            val unread =
                chapters
                    .list(mangaId)
                    .filter { !it.read && !it.downloaded && it.source.externalUrl == null }
                    .sortedBy { it.number }
                    .distinctBy { it.number }
            queue.enqueue((if (count == null) unread else unread.take(count)).map { it.id })
        }
    }

    /** RF-66: the files of these chapters go. */
    fun removeDownloads(ids: Collection<Long>) {
        val queue = downloads ?: return
        viewModelScope.launch { queue.remove(ids) }
        clearSelection()
    }

    /** RF-32: every chapter before [chapter] becomes read. */
    fun markPreviousAsRead(chapter: Chapter) {
        viewModelScope.launch {
            val ids = chapters.list(mangaId).filter { it.number < chapter.number }.map { it.id }
            chapters.setRead(ids, true)
            downloads?.chaptersRead(ids)
        }
    }

    private companion object {
        const val STOP_MS = 5_000L

        /** RF-117: a work refreshed less than 6 hours ago opens from what was saved. */
        const val FRESH_MS = 6 * 60 * 60 * 1_000L
    }
}
