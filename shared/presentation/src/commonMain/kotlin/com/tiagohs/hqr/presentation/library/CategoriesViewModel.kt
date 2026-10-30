package com.tiagohs.hqr.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.DeletedCategory
import com.tiagohs.hqr.domain.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** SCR-LIB-003: create, rename, order and delete categories (RF-53). */
class CategoriesViewModel(
    private val categories: CategoryRepository,
    mangas: MangaRepository? = null,
) : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val categories: List<Category> = emptyList(),
        /** Works in the library per category id. */
        val counts: Map<Long, Int> = emptyMap(),
    )

    /** Why a name can't be used. */
    enum class NameProblem { EMPTY, TAKEN }

    val state: StateFlow<State> =
        combine(categories.observe(), mangas?.observeLibrary() ?: flowOf(emptyList())) { list, items ->
            val counts = list.associate { cat -> cat.id to items.count { cat.id in it.categoryIds } }
            State(false, list, counts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), State())

    /** Null when [name] can be used; [except] is the category being renamed. */
    fun problem(
        name: String,
        except: Long? = null,
    ): NameProblem? {
        val clean = name.trim()
        return when {
            clean.isEmpty() -> NameProblem.EMPTY
            state.value.categories.any { it.id != except && it.name.equals(clean, ignoreCase = true) } -> NameProblem.TAKEN
            else -> null
        }
    }

    fun create(name: String) {
        if (problem(name) != null) return
        viewModelScope.launch { categories.create(name) }
    }

    fun rename(
        id: Long,
        name: String,
    ) {
        if (problem(name, except = id) != null) return
        viewModelScope.launch { categories.rename(id, name) }
    }

    /** Moves one place up (-1) or down (+1). */
    fun move(
        id: Long,
        by: Int,
    ) {
        val list = state.value.categories.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + by
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { categories.reorder(list.map { it.id }) }
    }

    /** Deletes and calls [onDeleted] with what "Desfazer" needs. */
    fun delete(
        id: Long,
        onDeleted: (DeletedCategory) -> Unit = {},
    ) {
        viewModelScope.launch { categories.delete(id)?.let(onDeleted) }
    }

    fun undoDelete(deleted: DeletedCategory) {
        viewModelScope.launch { categories.restore(deleted) }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
