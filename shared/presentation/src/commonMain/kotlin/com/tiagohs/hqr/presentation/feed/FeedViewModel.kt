package com.tiagohs.hqr.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.FeedItem
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.common.attempt
import com.tiagohs.hqr.presentation.common.toErrorKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The feed of SCR-EXP-001 (RF-25): up to 5 items (a source's latest or a saved search), their works in one list, the
 * most recently updated first and works without a date at the end. Loaded on opening and on pull, never in the
 * background.
 */
class FeedViewModel(
    private val searches: SearchRepository,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
) : ViewModel() {
    /** A work with where it came from: the source's name, or the saved search's. */
    data class Entry(
        val manga: SourceManga,
        val origin: String,
    )

    data class State(
        val items: List<FeedItem> = emptyList(),
        val entries: List<Entry> = emptyList(),
        val loading: Boolean = true,
        /** Origins that failed this time; the others still show (RF-05). */
        val failed: Map<String, ErrorKind> = emptyMap(),
    ) {
        val isEmpty: Boolean get() = !loading && items.isEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refresh()
        // Items added or removed elsewhere (a source's screen) show at the next load, and the list of items at once.
        viewModelScope.launch { searches.observeFeed().collect { items -> _state.update { it.copy(items = items) } } }
    }

    /** Opening and pulling: every item asked again. */
    fun refresh() {
        job?.cancel()
        job =
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                registry.awaitReady()
                val items = searches.observeFeed().first()
                val results =
                    items
                        .map { item -> async { origin(item) to attempt { load(item) } } }
                        .awaitAll()
                val entries =
                    results
                        .flatMap { (origin, result) -> result.getOrNull().orEmpty().map { Entry(it, origin) } }
                        .distinctBy { it.manga.sourceId to it.manga.key }
                        .sortedWith(
                            compareByDescending<Entry> { it.manga.updatedAtEpochMs != null }.thenByDescending {
                                it.manga.updatedAtEpochMs
                                    ?: 0
                            },
                        )
                val failed = results.mapNotNull { (origin, result) -> result.exceptionOrNull()?.let { origin to it.toErrorKind() } }.toMap()
                _state.update { it.copy(items = items, entries = entries, loading = false, failed = failed) }
            }
    }

    private fun origin(item: FeedItem): String = item.savedSearch?.name ?: registry[item.sourceId]?.name ?: item.sourceId.value

    private suspend fun load(item: FeedItem): List<SourceManga> {
        val source = registry.awaitRequire(item.sourceId)
        val search = item.savedSearch
        return if (search != null && source is SearchSource) {
            source.search(search.query, search.filters, 1).items
        } else {
            (source as? CatalogSource)?.latest(1)?.items.orEmpty()
        }
    }

    /** SCR-EXP-004: what can go in: every source's latest and every saved search. */
    val allSaved = searches.observeAllSaved()

    /** RF-25: false when the feed is full or already has it. */
    fun add(
        sourceId: com.tiagohs.hqr.domain.model.SourceId,
        savedSearchId: Long?,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val added = searches.addToFeed(sourceId, savedSearchId)
            onDone(added)
            if (added) refresh()
        }
    }

    fun remove(item: FeedItem) {
        viewModelScope.launch {
            searches.removeFromFeed(item.id)
            refresh()
        }
    }

    suspend fun open(manga: SourceManga): Long = mangas.upsert(manga).id
}
