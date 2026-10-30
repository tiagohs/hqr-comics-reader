package com.tiagohs.hqr.presentation.sources

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.CatalogBrowser
import com.tiagohs.hqr.domain.source.CatalogEntry
import com.tiagohs.hqr.domain.source.CatalogPage
import com.tiagohs.hqr.domain.source.SourceException
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

/**
 * SCR-SRV-002: a server's catalog, folder by folder, with its search; a book opens the feed that holds it as a work
 * (RF-18). The path of folders survives the end of the process (RF-88).
 */
class ServerCatalogViewModel(
    private val sourceId: SourceId,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val saved: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    data class State(
        val serverName: String = "",
        val page: CatalogPage? = null,
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: ErrorKind? = null,
        val canGoUp: Boolean = false,
        val query: String = "",
    ) {
        val isEmpty: Boolean get() = !loading && error == null && page?.entries?.isEmpty() == true
    }

    private val browser get() = registry[sourceId] as? CatalogBrowser

    // Saved as one string: every platform's saved state takes it (RF-88).
    private val path: MutableList<String> =
        saved
            .get<String>(KEY_PATH)
            ?.split(SEPARATOR)
            ?.filter { it.isNotEmpty() }
            ?.toMutableList() ?: mutableListOf()
    private val _state = MutableStateFlow(State(serverName = registry[sourceId]?.name.orEmpty(), query = saved[KEY_QUERY] ?: ""))
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null
    private val morePages = HashSet<String>()

    init {
        load()
    }

    /** A folder goes one level down; a book is handed to [onWork] as the id of its work. */
    fun open(
        entry: CatalogEntry,
        onWork: (Long) -> Unit,
    ) {
        if (!entry.isBook) {
            path += entry.url
            load()
            return
        }
        val catalog = browser ?: return
        // A book from a page brought by "Carregar mais" belongs to the feed's first page: the work starts there (RF-18).
        val page = _state.value.page
        val feedUrl = if (entry.url in morePages) page?.url ?: entry.url else entry.url
        viewModelScope.launch {
            val work = catalog.work(feedUrl, page?.title ?: entry.title)
            onWork(mangas.upsert(work).id)
        }
    }

    /** Back one folder; false at the root (the screen closes). */
    fun up(): Boolean {
        if (path.isEmpty()) return false
        path.removeAt(path.lastIndex)
        load()
        return true
    }

    fun search(query: String) {
        _state.update { it.copy(query = query) }
        saved[KEY_QUERY] = query
        path.clear()
        if (query.isNotBlank()) path += SEARCH_PREFIX + query.trim()
        load()
    }

    fun retry() = load()

    fun loadMore() {
        val current = _state.value
        val next = current.page?.nextUrl ?: return
        if (current.loadingMore || current.loading) return
        val catalog = browser ?: return
        _state.update { it.copy(loadingMore = true) }
        job =
            viewModelScope.launch {
                attempt { catalog.feed(next) }
                    .onSuccess { more ->
                        morePages += next
                        _state.update { s ->
                            s.copy(
                                page = s.page?.copy(entries = s.page.entries + more.entries, nextUrl = more.nextUrl),
                                loadingMore = false,
                            )
                        }
                    }.onFailure { e -> _state.update { it.copy(loadingMore = false, error = e.toErrorKind()) } }
            }
    }

    private fun load() {
        saved[KEY_PATH] = path.joinToString(SEPARATOR)
        morePages.clear()
        job?.cancel()
        _state.update { it.copy(loading = true, error = null, canGoUp = path.isNotEmpty()) }
        job =
            viewModelScope.launch {
                attempt {
                    registry.awaitReady()
                    val catalog = browser ?: throw SourceException("Servidor removido", SourceException.Kind.UNAVAILABLE)
                    _state.update { it.copy(serverName = catalog.name) }
                    val top = path.lastOrNull()
                    when {
                        top == null -> catalog.feed(null)
                        top.startsWith(SEARCH_PREFIX) -> catalog.searchCatalog(top.removePrefix(SEARCH_PREFIX))
                        else -> catalog.feed(top)
                    }
                }.onSuccess { page -> _state.update { it.copy(page = page, loading = false) } }
                    .onFailure { e -> _state.update { it.copy(loading = false, error = e.toErrorKind()) } }
            }
    }

    private companion object {
        const val KEY_PATH = "catalog.path"
        const val KEY_QUERY = "catalog.query"
        const val SEARCH_PREFIX = "search:"
        const val SEPARATOR = "\n"
    }
}
