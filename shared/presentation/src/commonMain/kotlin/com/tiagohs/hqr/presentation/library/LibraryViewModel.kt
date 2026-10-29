package com.tiagohs.hqr.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.LibraryItem
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
    categories: CategoryRepository,
    history: HistoryRepository,
) : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val tabs: List<Tab> = emptyList(),
        val selectedTab: Long = ALL,
        val items: List<LibraryItem> = emptyList(),
        val continueReading: HistoryEntry? = null,
    ) {
        val isEmpty: Boolean get() = !loading && tabs.firstOrNull()?.count == 0
    }

    /** [name] is empty for the built-in tabs ([ALL], [UNCATEGORIZED]); the screen names them in the app language. */
    data class Tab(
        val id: Long,
        val name: String,
        val count: Int,
    )

    private val selected = MutableStateFlow(ALL)

    val state: StateFlow<State> =
        combine(mangas.observeLibrary(), categories.observe(), history.observeLatestPerManga(), selected) { items, cats, latest, tab ->
            val tabs = tabs(items, cats)
            val current = tabs.firstOrNull { it.id == tab }?.id ?: ALL
            State(
                loading = false,
                tabs = tabs,
                selectedTab = current,
                items =
                    items.filter {
                        current == ALL || current in it.categoryIds || (current == UNCATEGORIZED && it.categoryIds.isEmpty())
                    },
                continueReading = latest.firstOrNull { entry -> !entry.chapter.read && items.any { it.manga.id == entry.manga.id } },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun selectTab(id: Long) {
        selected.value = id
    }

    fun remove(mangaId: Long) {
        viewModelScope.launch { mangas.setInLibrary(mangaId, false) }
    }

    fun undoRemove(mangaId: Long) {
        viewModelScope.launch { mangas.setInLibrary(mangaId, true) }
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
    }
}
