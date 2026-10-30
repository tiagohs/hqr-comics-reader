package com.tiagohs.hqr.presentation.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.ReadingRules
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The work screen (SCR-OBR-001): details, chapters, the main button and the chapter actions (RF-29 to RF-33). */
class MangaViewModel(
    private val mangaId: Long,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
    private val coverPixels: CoverPixels? = null,
    private val categories: CategoryRepository? = null,
) : ViewModel() {
    enum class Order { NEWEST_FIRST, OLDEST_FIRST }

    enum class ChapterFilter { ALL, UNREAD, DOWNLOADED }

    data class State(
        val manga: Manga? = null,
        val chapters: List<Chapter> = emptyList(),
        val action: ReadAction = ReadAction.Nothing,
        val refreshing: Boolean = true,
        val error: ErrorKind? = null,
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
            order,
            combine(filter, group, ::Pair),
        ) { manga, list, (busy, err, opened), ord, (fil, grp) ->
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
                order = ord,
                filter = fil,
                totalChapters = list.map { it.number }.distinct().size,
                group = grp,
                groups = list.mapNotNull { it.source.group }.distinct().sorted(),
                newChapterIds = if (opened == null) emptySet() else list.filter { it.fetchedAtEpochMs > opened }.map { it.id }.toSet(),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State())

    init {
        refresh(markOpened = true)
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
    fun refresh() = refresh(markOpened = false)

    /** The opening is recorded after the first refresh, so the chapters it brings are never "new" next time. */
    private fun refresh(markOpened: Boolean) {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            val manga = mangas.get(mangaId)
            if (manga == null) {
                error.value = ErrorKind.NOT_FOUND
                refreshing.value = false
                return@launch
            }
            attempt {
                val source = registry.require(manga.sourceId)
                val details = source.details(manga.source)
                mangas.upsert(details)
                chapters.sync(mangaId, source.chapters(details))
            }.onFailure { error.value = it.toErrorKind() }
            if (markOpened) previousOpen.value = mangas.markOpened(mangaId)
            refreshing.value = false
            // After the refresh: a cover changed by it is the one that gives the color.
            if (markOpened) colorFromCover()
        }
    }

    fun setCategories(ids: Set<Long>) {
        viewModelScope.launch { categories?.setCategories(mangaId, ids) }
    }

    fun toggleLibrary() {
        viewModelScope.launch {
            val manga = mangas.get(mangaId) ?: return@launch
            mangas.setInLibrary(mangaId, !manga.inLibrary)
        }
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
        viewModelScope.launch { chapters.setRead(ids, read) }
    }

    /** RF-32: every chapter before [chapter] becomes read. */
    fun markPreviousAsRead(chapter: Chapter) {
        viewModelScope.launch {
            val ids = chapters.list(mangaId).filter { it.number < chapter.number }.map { it.id }
            chapters.setRead(ids, true)
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
