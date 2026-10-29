package com.tiagohs.hqr.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceFilter
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

/** One source: Populares, Recentes and search with filters, with continuous loading (SCR-EXP-002, RF-21, RF-22). */
class BrowseSourceViewModel(
    private val sourceId: SourceId,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
) : ViewModel() {
    enum class Listing { POPULAR, LATEST, SEARCH }

    data class State(
        val sourceName: String = "",
        val listing: Listing = Listing.POPULAR,
        val query: String = "",
        val filters: List<SourceFilter> = emptyList(),
        val items: List<SourceManga> = emptyList(),
        val inLibrary: Set<String> = emptySet(),
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val hasNextPage: Boolean = false,
        val error: ErrorKind? = null,
        val canSearch: Boolean = false,
        val hasLatest: Boolean = true,
    ) {
        val isEmpty: Boolean get() = !loading && error == null && items.isEmpty()
    }

    private val source = registry.require(sourceId)
    private val _state =
        MutableStateFlow(
            State(
                sourceName = source.name,
                canSearch = source is SearchSource,
                filters = (source as? SearchSource)?.filters().orEmpty(),
            ),
        )
    val state: StateFlow<State> = _state.asStateFlow()
    private var page = 0
    private var job: Job? = null

    init {
        viewModelScope.launch { mangas.observeLibraryKeys(sourceId).collect { keys -> _state.update { it.copy(inLibrary = keys) } } }
        reload()
    }

    fun show(listing: Listing) {
        if (listing == _state.value.listing && listing != Listing.SEARCH) return
        _state.update { it.copy(listing = listing) }
        reload()
    }

    fun search(
        query: String,
        filters: List<SourceFilter> = _state.value.filters,
    ) {
        _state.update { it.copy(listing = Listing.SEARCH, query = query, filters = filters) }
        reload()
    }

    fun retry() = reload()

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasNextPage) return
        fetch(page + 1, append = true)
    }

    private fun reload() {
        page = 0
        _state.update { it.copy(items = emptyList(), loading = true, error = null, hasNextPage = false) }
        fetch(1, append = false)
    }

    private fun fetch(
        next: Int,
        append: Boolean,
    ) {
        job?.cancel()
        job =
            viewModelScope.launch {
                if (append) _state.update { it.copy(loadingMore = true) }
                val snapshot = _state.value
                attempt { load(snapshot, next) }
                    .onSuccess { result ->
                        page = next
                        _state.update { s ->
                            s.copy(
                                items =
                                    if (append) {
                                        s.items +
                                            result.items.filter { new ->
                                                s.items.none { it.key == new.key }
                                            }
                                    } else {
                                        result.items
                                    },
                                hasNextPage = result.hasNextPage,
                                loading = false,
                                loadingMore = false,
                                error = null,
                            )
                        }
                    }.onFailure { e ->
                        _state.update { it.copy(loading = false, loadingMore = false, error = e.toErrorKind()) }
                    }
            }
    }

    private suspend fun load(
        state: State,
        page: Int,
    ): MangaPage =
        when (state.listing) {
            Listing.POPULAR -> (source as CatalogSource).popular(page)
            Listing.LATEST -> (source as CatalogSource).latest(page)
            Listing.SEARCH -> (source as SearchSource).search(state.query, state.filters, page)
        }

    /** Saves the work locally before opening it, so the work screen reads one id. */
    suspend fun open(manga: SourceManga): Long = mangas.upsert(manga).id
}
