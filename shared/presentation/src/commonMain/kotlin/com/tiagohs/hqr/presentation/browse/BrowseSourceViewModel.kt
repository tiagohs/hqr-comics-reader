package com.tiagohs.hqr.presentation.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.repository.SavedSearch
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** One source: Populares, Recentes and search with filters, with continuous loading (SCR-EXP-002, RF-21, RF-22). */
@Suppress("TooManyFunctions") // One screen: listings, search, filters and saved searches.
class BrowseSourceViewModel(
    private val sourceId: SourceId,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    initialQuery: String? = null,
    private val saved: SavedStateHandle = SavedStateHandle(),
    private val searches: SearchRepository? = null,
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
        /** RF-15: pulled to refresh; the list stays on screen until the new one arrives. */
        val refreshing: Boolean = false,
        val hasNextPage: Boolean = false,
        val error: ErrorKind? = null,
        /** A failure while loading the next page: the list stays, the footer offers "Tentar de novo" (RF-21). */
        val loadMoreError: ErrorKind? = null,
        val canSearch: Boolean = false,
        val hasLatest: Boolean = true,
        /** "Abrir no navegador" in the error (RF-05). */
        val webUrl: String? = null,
    ) {
        val isEmpty: Boolean get() = !loading && error == null && items.isEmpty()

        /** How many filters differ from the defaults; they only apply to the search, so 0 elsewhere. */
        val activeFilters: Int get() = if (listing == Listing.SEARCH) filters.count { !it.isDefault } else 0
    }

    // A source removed or not registered yet (servers, RF-17) is an error on screen, never a crash.
    private val source = registry[sourceId]
    private val _state =
        MutableStateFlow(
            State(
                sourceName = source?.name ?: sourceId.value,
                canSearch = source is SearchSource,
                // RF-88: what was on screen before the end of the process.
                listing = saved.get<String>(KEY_LISTING)?.let { Listing.valueOf(it) } ?: Listing.POPULAR,
                query = saved[KEY_QUERY] ?: "",
                filters =
                    saved
                        .get<String>(KEY_FILTERS)
                        ?.let {
                            runCatching { filtersJson.decodeFromString<List<SourceFilter>>(it) }.getOrNull()
                        }.orEmpty(),
                webUrl = source?.webUrl(saved[KEY_QUERY] ?: ""),
            ),
        )
    val state: StateFlow<State> = _state.asStateFlow()
    private var page = 0
    private var job: Job? = null

    /** The source's filters at their defaults, for "Limpar" (RF-22). */
    private var defaultFilters: List<SourceFilter> = emptyList()

    init {
        viewModelScope.launch { mangas.observeLibraryKeys(sourceId).collect { keys -> _state.update { it.copy(inLibrary = keys) } } }
        // A failure here only hides the filters; the listing still works (RF-05).
        (source as? SearchSource)?.let { search ->
            viewModelScope.launch {
                attempt { search.filters() }.onSuccess { list ->
                    defaultFilters = list
                    _state.update { if (it.filters.isEmpty()) it.copy(filters = list) else it }
                }
            }
        }
        // From global search: the source opens on that search, never on Populares first.
        if (!initialQuery.isNullOrBlank() && source is SearchSource && saved.get<String>(KEY_LISTING) == null) {
            _state.update { it.copy(listing = Listing.SEARCH, query = initialQuery) }
        }
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

    /** RF-24: this source's saved searches. */
    val savedSearches: StateFlow<List<SavedSearch>> =
        (searches?.observeSaved(sourceId) ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    /** RF-24: the text and filters on screen, kept by [name]. */
    fun saveSearch(name: String) {
        val repo = searches ?: return
        val s = _state.value
        if (name.isBlank()) return
        viewModelScope.launch { repo.save(sourceId, name, s.query, s.filters) }
    }

    /** RF-24: one tap brings back the text and the filters, and the list is made again. */
    fun applySaved(search: SavedSearch) = search(search.query, search.filters.ifEmpty { defaultFilters })

    fun renameSaved(
        search: SavedSearch,
        name: String,
    ) {
        viewModelScope.launch { searches?.rename(search.id, name) }
    }

    fun deleteSaved(search: SavedSearch) {
        viewModelScope.launch { searches?.delete(search.id) }
    }

    /** RF-25: this source's latest, or one of its saved searches, into the feed; false when the feed is full. */
    suspend fun addToFeed(search: SavedSearch?): Boolean = searches?.addToFeed(sourceId, search?.id) ?: false

    /** RF-22: the sheet's "Buscar": the same text with the new filters. */
    fun applyFilters(filters: List<SourceFilter>) = search(_state.value.query, filters)

    /** Filters back to what the source gives, without searching yet. */
    fun defaultFilters(): List<SourceFilter> = defaultFilters

    /** Next page at the end of the list; also the footer's "Tentar de novo" after a failure. */
    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasNextPage) return
        fetch(page + 1, append = true)
    }

    /** RF-15: pull to refresh — the source is read again (a local folder: new and removed files show up). */
    fun refresh() {
        if (_state.value.loading || _state.value.refreshing) return
        page = 0
        _state.update { it.copy(refreshing = true, loadMoreError = null) }
        fetch(1, append = false)
    }

    private fun reload() {
        page = 0
        _state.update { it.copy(items = emptyList(), loading = true, error = null, loadMoreError = null, hasNextPage = false) }
        persist()
        fetch(1, append = false)
    }

    private fun persist() {
        val s = _state.value
        saved[KEY_LISTING] = s.listing.name
        saved[KEY_QUERY] = s.query
        saved[KEY_FILTERS] = filtersJson.encodeToString(s.filters)
        _state.update { it.copy(webUrl = source?.webUrl(if (it.listing == Listing.SEARCH) it.query else "")) }
    }

    private fun fetch(
        next: Int,
        append: Boolean,
    ) {
        job?.cancel()
        job =
            viewModelScope.launch {
                if (append) _state.update { it.copy(loadingMore = true, loadMoreError = null) }
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
                                refreshing = false,
                                error = null,
                                loadMoreError = null,
                            )
                        }
                    }.onFailure { e ->
                        _state.update {
                            if (append) {
                                it.copy(loadingMore = false, loadMoreError = e.toErrorKind())
                            } else {
                                // A failed pull keeps the list on screen; the error shows only when there is nothing,
                                // or when the source wants a verification (RF-106), which needs its own screen.
                                val kind = e.toErrorKind()
                                val keep = it.refreshing && it.items.isNotEmpty() && kind != ErrorKind.CHALLENGE
                                it.copy(
                                    loading = false,
                                    loadingMore = false,
                                    refreshing = false,
                                    error = if (keep) null else kind,
                                    loadMoreError = if (keep) kind else null,
                                )
                            }
                        }
                    }
            }
    }

    private suspend fun load(
        state: State,
        page: Int,
    ): MangaPage =
        when (state.listing) {
            Listing.POPULAR -> (registry.awaitRequire(sourceId) as CatalogSource).popular(page)
            Listing.LATEST -> (registry.awaitRequire(sourceId) as CatalogSource).latest(page)
            Listing.SEARCH -> (registry.awaitRequire(sourceId) as SearchSource).search(state.query, state.filters, page)
        }

    /** RF-52: a long press puts the work in the library or takes it out; null means it went in. */
    suspend fun toggleLibrary(manga: SourceManga): LibraryRemoval? = mangas.toggleInLibrary(manga)

    suspend fun undoToggle(
        manga: SourceManga,
        removal: LibraryRemoval?,
    ) = mangas.undoToggle(manga, removal)

    /** Saves the work locally before opening it, so the work screen reads one id. */
    suspend fun open(manga: SourceManga): Long = mangas.upsert(manga).id

    private companion object {
        const val KEY_LISTING = "browse.listing"
        const val KEY_QUERY = "browse.query"
        const val KEY_FILTERS = "browse.filters"
        const val STOP_MS = 5_000L
        val filtersJson = Json { ignoreUnknownKeys = true }
    }
}
