package com.tiagohs.hqr.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.CacheStore
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * The feed of SCR-EXP-001 (RF-25): up to 5 items (a source's latest or a saved search), their works in one list, the
 * most recently updated first and works without a date at the end. RF-116: it opens with the first page each item had
 * the last time and asks again only the items older than [STALE_MS]; pulling asks every item.
 */
class FeedViewModel(
    private val searches: SearchRepository,
    private val registry: SourceRegistry,
    private val mangas: MangaRepository,
    private val cache: CacheStore = CacheStore.None,
    private val clock: Clock = Clock.System,
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
        /** RF-116: asking the sources again behind what is already on screen. */
        val refreshing: Boolean = false,
        /** RF-116: when the oldest item shown was asked; null before anything was saved. */
        val updatedAtEpochMs: Long? = null,
    ) {
        val isEmpty: Boolean get() = !loading && items.isEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load(force = false)
        // Items added or removed elsewhere (a source's screen) show at the next load, and the list of items at once.
        viewModelScope.launch { searches.observeFeed().collect { items -> _state.update { it.copy(items = items) } } }
    }

    /** Pulling: every item asked again. */
    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        job?.cancel()
        job =
            viewModelScope.launch {
                registry.awaitReady()
                val items = searches.observeFeed().first()
                val now = clock.now().toEpochMilliseconds()
                val saved =
                    items.associateWith { item ->
                        cache.read(key(item))?.let {
                            decode(it.text)?.let { list ->
                                list to
                                    it.savedAtEpochMs
                            }
                        }
                    }
                val stale = items.filter { item -> force || saved[item]?.let { now - it.second >= STALE_MS } != false }
                _state.update {
                    it.copy(
                        items = items,
                        entries = merge(items.map { item -> origin(item) to Result.success(saved[item]?.first.orEmpty()) }),
                        loading = saved.values.all { v -> v == null } && stale.isNotEmpty(),
                        refreshing = stale.isNotEmpty(),
                        updatedAtEpochMs = saved.values.mapNotNull { v -> v?.second }.minOrNull(),
                    )
                }
                if (stale.isEmpty()) return@launch
                val asked =
                    stale
                        .map { item -> async { item to attempt { fetch(item) } } }
                        .awaitAll()
                        .toMap()
                asked.forEach { (item, result) -> result.getOrNull()?.let { cache.write(key(item), json.encodeToString(LIST, it)) } }
                val results =
                    items.map { item ->
                        val fetched = asked[item]
                        origin(item) to
                            when {
                                fetched == null -> Result.success(saved[item]?.first.orEmpty())
                                fetched.isSuccess -> fetched
                                // RF-05: a failed item keeps what it had and says so.
                                else -> saved[item]?.first?.let { Result.success(it) } ?: fetched
                            }
                    }
                val failed =
                    asked.mapNotNull { (item, result) -> result.exceptionOrNull()?.let { origin(item) to it.toErrorKind() } }.toMap()
                val times = items.mapNotNull { item -> cache.read(key(item))?.savedAtEpochMs }
                _state.update {
                    it.copy(
                        items = items,
                        entries = merge(results),
                        loading = false,
                        refreshing = false,
                        failed = failed,
                        updatedAtEpochMs = times.minOrNull(),
                    )
                }
            }
    }

    private fun merge(results: List<Pair<String, Result<List<SourceManga>>>>): List<Entry> =
        results
            .flatMap { (origin, result) -> result.getOrNull().orEmpty().map { Entry(it, origin) } }
            .distinctBy { it.manga.sourceId to it.manga.key }
            .sortedWith(
                compareByDescending<Entry> { it.manga.updatedAtEpochMs != null }.thenByDescending { it.manga.updatedAtEpochMs ?: 0 },
            )

    private fun key(item: FeedItem): String = "feed.${item.sourceId.value}.${item.savedSearch?.id ?: "latest"}"

    private fun decode(text: String): List<SourceManga>? = runCatching { json.decodeFromString(LIST, text) }.getOrNull()

    private fun origin(item: FeedItem): String = item.savedSearch?.name ?: registry[item.sourceId]?.name ?: item.sourceId.value

    private suspend fun fetch(item: FeedItem): List<SourceManga> {
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

    private companion object {
        /** RF-116: older than 30 minutes, an item is asked again when the feed opens. */
        const val STALE_MS = 30 * 60 * 1_000L
        val LIST = ListSerializer(SourceManga.serializer())
        val json = Json { ignoreUnknownKeys = true }
    }
}
