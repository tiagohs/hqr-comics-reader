package com.tiagohs.hqr.presentation.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The library tab (SCR-LIB-001): categories as tabs, the grid and "Continuar lendo". */
class LibraryViewModel(
    private val mangas: MangaRepository,
    private val categories: CategoryRepository,
    history: HistoryRepository,
    private val optionsStore: LibraryOptionsStore? = null,
    private val saved: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val tabs: List<Tab> = emptyList(),
        val selectedTab: Long = ALL,
        val items: List<LibraryItem> = emptyList(),
        val continueReading: HistoryEntry? = null,
        val options: LibraryOptions = LibraryOptions(),
        val query: String = "",
        val categories: List<Category> = emptyList(),
        val sourceIds: List<String> = emptyList(),
    ) {
        val isEmpty: Boolean get() = !loading && tabs.firstOrNull()?.count == 0

        /** Works exist but the filters or the search hide all of them. */
        val isFilteredOut: Boolean get() = !loading && !isEmpty && items.isEmpty()
    }

    /** [name] is empty for the built-in tabs ([ALL], [UNCATEGORIZED]); the screen names them in the app language. */
    data class Tab(
        val id: Long,
        val name: String,
        val count: Int,
    )

    // RF-88: the tab and the search survive the end of the process.
    private val selected = saved.getStateFlow(KEY_TAB, ALL)
    private val options = MutableStateFlow(optionsStore?.load() ?: LibraryOptions())
    private val query = saved.getStateFlow(KEY_QUERY, "")

    val state: StateFlow<State> =
        combine(
            combine(mangas.observeLibrary(), categories.observe(), ::Pair),
            history.observeLatestPerManga(),
            selected,
            options,
            query,
        ) { (items, cats), latest, tab, opts, text ->
            val tabs = tabs(items, cats)
            // A filter on a category or source that no longer exists is dropped, never left hiding everything.
            val sources = items.map { it.manga.sourceId.value }.toSet()
            val valid =
                opts.copy(
                    categories = opts.categories.filter { id -> id == Category.DEFAULT_ID || cats.any { it.id == id } }.toSet(),
                    sources = opts.sources.intersect(sources),
                )
            val libraryIds = items.mapTo(HashSet()) { it.manga.id }
            val current = tabs.firstOrNull { it.id == tab }?.id ?: ALL
            val inTab =
                items.filter {
                    current == ALL || current in it.categoryIds || (current == UNCATEGORIZED && it.categoryIds.isEmpty())
                }
            State(
                loading = false,
                tabs = tabs,
                selectedTab = current,
                items = LibraryRules.apply(inTab, valid, text),
                continueReading = latest.firstOrNull { entry -> !entry.chapter.read && entry.manga.id in libraryIds },
                options = valid,
                query = text,
                categories = cats,
                sourceIds = items.map { it.manga.sourceId.value }.distinct().sorted(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun selectTab(id: Long) {
        saved[KEY_TAB] = id
    }

    /** RF-54, RF-55: saved at once, so the next opening shows the library the same way. */
    fun setOptions(value: LibraryOptions) {
        options.value = value
        optionsStore?.save(value)
    }

    /** RF-56. */
    fun search(text: String) {
        saved[KEY_QUERY] = text
    }

    /** RF-52: takes the work out; [onRemoved] gets what "Desfazer" needs (date added and categories). */
    fun remove(
        item: LibraryItem,
        onRemoved: (LibraryRemoval) -> Unit,
    ) {
        viewModelScope.launch { mangas.removeFromLibrary(item.manga.id)?.let(onRemoved) }
    }

    fun undoRemove(removal: LibraryRemoval) {
        viewModelScope.launch { mangas.restoreToLibrary(removal) }
    }

    private fun tabs(
        items: List<LibraryItem>,
        categories: List<Category>,
    ): List<Tab> {
        val uncategorized = items.count { it.categoryIds.isEmpty() }
        return buildList {
            add(Tab(ALL, "", items.size))
            categories.forEach { cat -> add(Tab(cat.id, cat.name, items.count { cat.id in it.categoryIds })) }
            // RF-53: "Sem categoria" only shows when some work has none and there are categories.
            if (categories.isNotEmpty() && uncategorized > 0) add(Tab(UNCATEGORIZED, "", uncategorized))
        }
    }

    companion object {
        const val ALL = -1L
        const val UNCATEGORIZED = Category.DEFAULT_ID
        private const val KEY_TAB = "library.tab"
        private const val KEY_QUERY = "library.query"
    }
}
