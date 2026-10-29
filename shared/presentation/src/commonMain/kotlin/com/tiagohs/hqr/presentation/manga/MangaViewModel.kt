package com.tiagohs.hqr.presentation.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.ReadingRules
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The work screen (SCR-OBR-001): details, chapters, the main button and the chapter actions (RF-29 to RF-33). */
class MangaViewModel(
    private val mangaId: Long,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
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
    )

    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<ErrorKind?>(null)
    private val order = MutableStateFlow(Order.NEWEST_FIRST)
    private val filter = MutableStateFlow(ChapterFilter.ALL)

    val state: StateFlow<State> =
        combine(
            mangas.observe(mangaId),
            chapters.observe(mangaId),
            combine(refreshing, error, ::Pair),
            order,
            filter,
        ) { manga, list, (busy, err), ord, fil ->
            val visible =
                list
                    .filter {
                        when (fil) {
                            ChapterFilter.ALL -> true
                            ChapterFilter.UNREAD -> !it.read
                            ChapterFilter.DOWNLOADED -> it.downloaded
                        }
                    }.let { if (ord == Order.NEWEST_FIRST) it.sortedByDescending(Chapter::number) else it.sortedBy(Chapter::number) }
            State(
                manga = manga,
                chapters = visible,
                action = ReadingRules.readAction(list),
                refreshing = busy,
                error = err,
                order = ord,
                filter = fil,
                totalChapters = list.map { it.number }.distinct().size,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State())

    init {
        refresh()
    }

    /** RF-33: rereads details and chapters; on failure keeps what was there and warns. */
    fun refresh() {
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
            refreshing.value = false
        }
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
}
