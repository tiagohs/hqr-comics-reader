package com.tiagohs.hqr.presentation.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * SCR-SRCH-001: one query sent to every source with search, each in its own group (RF-23). A slow or broken source
 * never holds the others; each group has its own loading, error with "Tentar de novo" and empty (RF-05, RF-28).
 */
class GlobalSearchViewModel(
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val saved: SavedStateHandle = SavedStateHandle(),
    private val debounce: Duration = DEBOUNCE,
    private val searches: SearchRepository? = null,
) : ViewModel() {
    /** RF-27: the last 10 global searches, shown while the field is empty. */
    val recent: StateFlow<List<String>> =
        (searches?.observeRecent() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(RECENT_STOP_MS), emptyList())

    fun removeRecent(query: String) {
        viewModelScope.launch { searches?.removeRecent(query) }
    }

    /** A search the user meant (the keyboard's key, or opening one of its results) is kept; half-typed text isn't. */
    private fun remember(query: String) {
        if (query.isNotBlank()) viewModelScope.launch { searches?.addRecent(query) }
    }

    /** RF-27: a recent search, asked again. */
    fun searchAgain(query: String) {
        type(query)
        submit()
    }

    sealed interface GroupState {
        data object Loading : GroupState

        data class Error(
            val kind: ErrorKind,
        ) : GroupState

        data class Results(
            val items: List<SourceManga>,
        ) : GroupState
    }

    data class Group(
        val sourceId: SourceId,
        val sourceName: String,
        val state: GroupState,
        /** Keys of this source already in the library (RF-26). */
        val inLibrary: Set<String> = emptySet(),
    ) {
        val isEmpty: Boolean get() = (state as? GroupState.Results)?.items?.isEmpty() == true
    }

    data class State(
        val query: String = "",
        /** The query the groups answer, after the pause in typing. */
        val searched: String = "",
        val groups: List<Group> = emptyList(),
    ) {
        val searching: Boolean get() = groups.any { it.state == GroupState.Loading }

        /** RF-28: every source answered and none found anything. */
        val nothingFound: Boolean
            get() = searched.isNotBlank() && groups.isNotEmpty() && groups.all { it.isEmpty }
    }

    // RF-03: only the sources that are on.
    private val sources get() = registry.active.value.filterIsInstance<SearchSource>()
    private val _state = MutableStateFlow(State(query = saved[KEY_QUERY] ?: ""))
    val state: StateFlow<State> = _state.asStateFlow()
    private var debounceJob: Job? = null
    private val jobs = HashMap<SourceId, Job>()
    private var libraryKeys: Map<SourceId, Set<String>> = emptyMap()

    init {
        observeLibrary()
        // RF-88: the screen comes back with its results after the end of the process.
        if (_state.value.query.isNotBlank()) runSearch(_state.value.query)
    }

    /** Called on every key: waits for a pause of 400 ms before asking the sources (RF-23). */
    fun type(text: String) {
        _state.update { it.copy(query = text) }
        saved[KEY_QUERY] = text
        debounceJob?.cancel()
        debounceJob =
            viewModelScope.launch {
                delay(debounce)
                runSearch(text)
            }
    }

    /** The keyboard's search key: no wait; with the same text, the sources that failed are asked again. */
    fun submit() {
        debounceJob?.cancel()
        val current = _state.value
        remember(current.query)
        if (current.query.trim() == current.searched && current.groups.isNotEmpty()) {
            current.groups.filter { it.state is GroupState.Error }.forEach { retry(it.sourceId) }
        } else {
            runSearch(current.query)
        }
    }

    fun retry(sourceId: SourceId) {
        val source = sources.firstOrNull { it.id == sourceId } ?: return
        launchGroup(source, _state.value.searched)
    }

    private fun runSearch(text: String) {
        val query = text.trim()
        if (query == _state.value.searched && _state.value.groups.isNotEmpty() && query.isNotEmpty()) return
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        if (query.isEmpty()) {
            _state.update { it.copy(searched = "", groups = emptyList()) }
            return
        }
        val list = sources
        _state.update { s ->
            s.copy(
                searched = query,
                groups =
                    list.map { src ->
                        Group(src.id, src.name, GroupState.Loading, libraryKeys[src.id].orEmpty())
                    },
            )
        }
        list.forEach { launchGroup(it, query) }
    }

    private fun launchGroup(
        source: SearchSource,
        query: String,
    ) {
        jobs[source.id]?.cancel()
        setGroup(source.id) { it.copy(state = GroupState.Loading) }
        jobs[source.id] =
            viewModelScope.launch {
                val result = attempt { source.search(query, emptyList(), 1) }
                if (_state.value.searched != query) return@launch
                setGroup(source.id) { group ->
                    group.copy(
                        state =
                            result.fold(
                                { GroupState.Results(it.items.distinctBy { m -> m.key }) },
                                { GroupState.Error(it.toErrorKind()) },
                            ),
                    )
                }
            }
    }

    private fun setGroup(
        id: SourceId,
        change: (Group) -> Group,
    ) {
        _state.update { s -> s.copy(groups = s.groups.map { if (it.sourceId == id) change(it) else it }) }
    }

    private fun observeLibrary() {
        val ids = sources.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            combine(ids.map { mangas.observeLibraryKeys(it) }) { it.toList() }.collect { keys ->
                libraryKeys = ids.zip(keys).toMap()
                _state.update { s -> s.copy(groups = s.groups.map { g -> g.copy(inLibrary = libraryKeys[g.sourceId].orEmpty()) }) }
            }
        }
    }

    /** RF-52: long press on a result, as in the source screen. */
    suspend fun toggleLibrary(manga: SourceManga): LibraryRemoval? = mangas.toggleInLibrary(manga)

    suspend fun undoToggle(
        manga: SourceManga,
        removal: LibraryRemoval?,
    ) = mangas.undoToggle(manga, removal)

    /** RF-05: "Abrir no navegador" for a group in error. */
    fun webUrl(sourceId: SourceId): String? = registry[sourceId]?.webUrl(_state.value.searched)

    /** Saves the work before opening it, so the work screen reads one id. */
    suspend fun open(manga: SourceManga): Long {
        remember(_state.value.searched)
        return mangas.upsert(manga).id
    }

    companion object {
        private const val RECENT_STOP_MS = 5_000L
        val DEBOUNCE = 400.milliseconds
        private const val KEY_QUERY = "search.query"
    }
}
