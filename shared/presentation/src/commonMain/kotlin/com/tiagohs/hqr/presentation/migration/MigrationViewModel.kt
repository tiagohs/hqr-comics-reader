package com.tiagohs.hqr.presentation.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SCR-OBR-002: searches the work's title in the chosen sources, shows the candidates and migrates to one (RF-69). */
class MigrationViewModel(
    private val mangaId: Long,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val migration: WorkMigration,
) : ViewModel() {
    /** The candidates of one source. */
    data class Results(
        val loading: Boolean = true,
        val items: List<SourceManga> = emptyList(),
        val error: ErrorKind? = null,
    )

    data class SourceOption(
        val id: SourceId,
        val name: String,
        val chosen: Boolean,
    )

    data class State(
        val title: String = "",
        val fromSource: SourceId? = null,
        val query: String = "",
        val sources: List<SourceOption> = emptyList(),
        val results: Map<SourceId, Results> = emptyMap(),
        /** The candidate waiting for the confirmation. */
        val picked: SourceManga? = null,
        val migrating: Boolean = false,
        val error: ErrorKind? = null,
        /** The migration went through: the new work to open. */
        val done: WorkMigration.Result? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val jobs = mutableMapOf<SourceId, Job>()

    init {
        viewModelScope.launch {
            registry.awaitReady()
            val manga = mangas.get(mangaId)
            if (manga == null) {
                _state.update { it.copy(error = ErrorKind.NOT_FOUND) }
                return@launch
            }
            // Every online source that searches, other than the one the work is in (local files have no search).
            val options =
                registry.active.value
                    .filterIsInstance<SearchSource>()
                    .filter { it.id != manga.sourceId && it.id != SourceId.LOCAL }
                    .map { SourceOption(it.id, it.name, chosen = true) }
            _state.update { it.copy(title = manga.title, fromSource = manga.sourceId, query = manga.title, sources = options) }
            search()
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    /** A source in or out of the search; one turned on searches right away. */
    fun toggleSource(id: SourceId) {
        val on =
            _state.value.sources
                .firstOrNull { it.id == id }
                ?.chosen
                ?.not() ?: return
        _state.update { s -> s.copy(sources = s.sources.map { if (it.id == id) it.copy(chosen = on) else it }) }
        if (on) search(id) else drop(id)
    }

    fun search() {
        _state.value.sources.forEach { if (it.chosen) search(it.id) else drop(it.id) }
    }

    private fun drop(id: SourceId) {
        jobs.remove(id)?.cancel()
        _state.update { it.copy(results = it.results - id) }
    }

    private fun search(id: SourceId) {
        val source = registry[id] as? SearchSource ?: return
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        jobs[id]?.cancel()
        _state.update { it.copy(results = it.results + (id to Results())) }
        jobs[id] =
            viewModelScope.launch {
                val result = attempt { source.search(query, emptyList(), 1) }
                _state.update { s ->
                    s.copy(
                        results =
                            s.results +
                                (
                                    id to
                                        result.fold(
                                            { Results(loading = false, items = it.items) },
                                            { Results(loading = false, error = it.toErrorKind()) },
                                        )
                                ),
                    )
                }
            }
    }

    fun pick(candidate: SourceManga?) = _state.update { it.copy(picked = candidate, error = null) }

    /** RF-69: after the confirmation; [deleteDownloads] is the optional removal of the old files. */
    fun migrate(deleteDownloads: Boolean) {
        val target = _state.value.picked ?: return
        if (_state.value.migrating) return
        _state.update { it.copy(migrating = true, error = null) }
        viewModelScope.launch {
            attempt { migration.migrate(mangaId, target, deleteDownloads) }
                .onSuccess { result -> _state.update { it.copy(migrating = false, picked = null, done = result) } }
                .onFailure { e -> _state.update { it.copy(migrating = false, error = e.toErrorKind()) } }
        }
    }
}
