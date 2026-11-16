package com.tiagohs.hqr.presentation.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.ChapterReadState
import com.tiagohs.hqr.domain.model.ReadingMark
import com.tiagohs.hqr.domain.model.ReadingMarks
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RF-126 (SCR-LIB-004): marks what was read of a work without opening the reader. A work that has no chapter list yet
 * (put in the library straight from a grid) asks its source first.
 */
class MarkReadingViewModel(
    private val mangaId: Long,
    private val mangas: MangaRepository,
    private val chapters: ChapterRepository,
    private val registry: SourceRegistry,
    private val downloads: Downloads? = null,
) : ViewModel() {
    sealed interface State {
        val title: String

        data class Loading(
            override val title: String = "",
        ) : State

        data class Failed(
            override val title: String,
            val error: ErrorKind,
        ) : State

        /** [chapters] are the slider's steps, one per number in reading order; [index] is the step shown. */
        data class Ready(
            override val title: String,
            val chapters: List<Chapter>,
            val all: List<Chapter>,
            val mark: ReadingMark,
            val index: Int,
        ) : State {
            val total: Int get() = all.size
            val readCount: Int get() = ReadingMarks.plan(all, mark).read.size
            val chapter: Chapter? get() = chapters.getOrNull(index)
        }
    }

    /** What was saved, for the message and "Desfazer". */
    data class Saved(
        val title: String,
        val mark: ReadingMark,
        val chapterNumber: Double?,
        val readCount: Int,
        val total: Int,
    )

    private val _state = MutableStateFlow<State>(State.Loading())
    val state: StateFlow<State> = _state.asStateFlow()

    private var before: List<ChapterReadState> = emptyList()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            val manga = mangas.get(mangaId) ?: return@launch
            _state.value = State.Loading(manga.title)
            var list = chapters.list(mangaId)
            if (list.isEmpty()) {
                val fetched =
                    attempt {
                        val source = registry.awaitRequire(manga.sourceId)
                        chapters.sync(mangaId, source.chapters(manga.source))
                    }
                fetched.exceptionOrNull()?.let {
                    _state.value = State.Failed(manga.title, it.toErrorKind())
                    return@launch
                }
                list = fetched.getOrDefault(emptyList())
            }
            val steps = ReadingMarks.ordered(list)
            val mark = ReadingMarks.current(list)
            _state.value = State.Ready(manga.title, steps, list, mark, indexOf(steps, list, mark))
        }
    }

    private fun indexOf(
        steps: List<Chapter>,
        all: List<Chapter>,
        mark: ReadingMark,
    ): Int {
        if (all.isEmpty()) return 0
        return when (mark) {
            is ReadingMark.UpTo -> {
                val number = all.firstOrNull { it.id == mark.chapterId }?.number
                steps.indexOfFirst { it.number == number }.coerceAtLeast(0)
            }
            ReadingMark.All -> steps.lastIndex
            ReadingMark.None -> 0
        }
    }

    fun choose(mark: ReadingMark) {
        val ready = _state.value as? State.Ready ?: return
        _state.value =
            when (mark) {
                is ReadingMark.UpTo -> ready.copy(mark = mark, index = indexOf(ready.chapters, ready.all, mark))
                else -> ready.copy(mark = mark)
            }
    }

    /** The slider or the − and + buttons: also picks "lido até um capítulo". */
    fun chooseUpTo(index: Int) {
        val ready = _state.value as? State.Ready ?: return
        val step = ready.chapters.getOrNull(index.coerceIn(0, ready.chapters.lastIndex.coerceAtLeast(0))) ?: return
        _state.value = ready.copy(mark = ReadingMark.UpTo(step.id), index = ready.chapters.indexOf(step))
    }

    fun save(onSaved: (Saved) -> Unit) {
        val ready = _state.value as? State.Ready ?: return
        viewModelScope.launch {
            before = chapters.readStates(mangaId)
            val plan = ReadingMarks.plan(ready.all, ready.mark)
            if (plan.unread.isNotEmpty()) chapters.setRead(plan.unread, false)
            if (plan.read.isNotEmpty()) {
                chapters.setRead(plan.read, true)
                downloads?.chaptersRead(plan.read)
            }
            onSaved(Saved(ready.title, ready.mark, ready.chapter?.number, plan.read.size, ready.total))
        }
    }

    fun undo() {
        val states = before
        if (states.isEmpty()) return
        viewModelScope.launch { chapters.restore(states) }
    }
}
