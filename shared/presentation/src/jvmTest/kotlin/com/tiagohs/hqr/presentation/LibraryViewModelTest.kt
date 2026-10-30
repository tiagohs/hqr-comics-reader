package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryOptionsStore
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.library.TriState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LibraryViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val source = FakeSource()

    private class MemoryOptions : LibraryOptionsStore {
        var saved = LibraryOptions()

        override fun load() = saved

        override fun save(options: LibraryOptions) {
            saved = options
        }
    }

    private suspend fun addWorks(): List<Long> =
        source
            .popular(1)
            .items
            .map { db.mangas.upsert(it).id }
            .onEach { db.mangas.setInLibrary(it, true) }

    @Test
    fun RF52_removeAndUndoKeepsTheCategories() =
        runTest {
            val (a, _) = addWorks()
            val reading = db.categories.create("Lendo")
            val done = db.categories.create("Concluídas")
            db.categories.setCategories(a, setOf(reading.id, done.id))
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history))
            val item =
                vm.state
                    .first { it.items.size == 2 }
                    .items
                    .first { it.manga.id == a }
            val removed = vm.remove(item)
            vm.state.first { it.items.size == 1 }
            vm.undoRemove(removed)
            vm.state
                .first { it.items.size == 2 }
                .items
                .first { it.manga.id == a }
                .categoryIds shouldBe setOf(reading.id, done.id)
        }

    @Test
    fun RF53_deletingACategoryMovesItsWorksToUncategorized() =
        runTest {
            val (a, b) = addWorks()
            val categories = track(CategoriesViewModel(db.categories))
            categories.create("Lendo")
            val reading =
                categories.state
                    .first { it.categories.size == 1 }
                    .categories
                    .single()
            categories.problem("LENDO") shouldBe CategoriesViewModel.NameProblem.TAKEN
            categories.problem("  ") shouldBe CategoriesViewModel.NameProblem.EMPTY
            db.categories.setCategories(a, setOf(reading.id))
            val library = track(LibraryViewModel(db.mangas, db.categories, db.history))
            library.state
                .first { s -> s.tabs.any { it.id == reading.id } }
                .tabs
                .map { it.id } shouldContainExactly
                listOf(LibraryViewModel.ALL, reading.id, LibraryViewModel.UNCATEGORIZED)
            categories.delete(reading.id)
            val tabs = library.state.first { s -> s.tabs.none { it.id == reading.id } }.tabs
            tabs.map { it.id } shouldContainExactly listOf(LibraryViewModel.ALL)
            library.selectTab(LibraryViewModel.ALL)
            library.state
                .first { it.items.size == 2 }
                .items
                .map { it.manga.id }
                .toSet() shouldBe setOf(a, b)
        }

    @Test
    fun RF53_undoDeleteBringsTheCategoryAndItsWorksBack() =
        runTest {
            val (a, _) = addWorks()
            val categories = track(CategoriesViewModel(db.categories))
            categories.create("Lendo")
            val reading =
                categories.state
                    .first { it.categories.size == 1 }
                    .categories
                    .single()
            db.categories.setCategories(a, setOf(reading.id))
            var deleted: com.tiagohs.hqr.domain.repository.DeletedCategory? = null
            categories.delete(reading.id) { deleted = it }
            categories.state.first { it.categories.isEmpty() }
            deleted!!.mangaIds shouldBe setOf(a)
            categories.undoDelete(deleted!!)
            categories.state
                .first { it.categories.size == 1 }
                .categories
                .single()
                .name shouldBe "Lendo"
            db.categories.observeFor(a).first() shouldBe setOf(reading.id)
        }

    @Test
    fun RF52_RF53_longPressAddsAndTheWorkGoesInSeveralCategories() =
        runTest {
            val browse =
                track(
                    com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel(
                        source.id,
                        com.tiagohs.hqr.domain.source
                            .SourceRegistry(listOf(source)),
                        db.mangas,
                    ),
                )
            val first =
                browse.state
                    .first { it.items.isNotEmpty() }
                    .items
                    .first()
            browse.toggleLibrary(first) shouldBe true
            browse.state.first { first.key in it.inLibrary }
            val id = db.mangas.find(first.sourceId, first.key)!!.id
            val a = db.categories.create("Lendo")
            val b = db.categories.create("Acompanhando")
            val work =
                track(
                    com.tiagohs.hqr.presentation.manga.MangaViewModel(
                        id,
                        com.tiagohs.hqr.domain.source
                            .SourceRegistry(listOf(source)),
                        db.mangas,
                        db.chapters,
                        categories = db.categories,
                    ),
                )
            work.setCategories(setOf(a.id, b.id))
            work.categoryChoice
                .first { it.second == setOf(a.id, b.id) }
                .first
                .map { it.name } shouldContainExactly
                listOf("Lendo", "Acompanhando")
            browse.toggleLibrary(first) shouldBe false
        }

    @Test
    fun RF53_renameAndReorder() =
        runTest {
            val categories = track(CategoriesViewModel(db.categories))
            categories.create("A")
            categories.state.first { it.categories.size == 1 }
            categories.create("B")
            val two = categories.state.first { it.categories.size == 2 }.categories
            categories.move(two[1].id, -1)
            categories.state.first { s -> s.categories.map { it.name } == listOf("B", "A") }
            categories.rename(two[0].id, "Lendo")
            categories.state.first { s -> s.categories.map { it.name } == listOf("B", "Lendo") }
        }

    @Test
    fun RF55_RF56_optionsAreSavedAndSearchFilters() =
        runTest {
            addWorks()
            val store = MemoryOptions()
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history, store))
            vm.state.first { it.items.size == 2 }
            vm.setOptions(LibraryOptions(unread = TriState.ONLY))
            store.saved.unread shouldBe TriState.ONLY
            // No chapters were synced: nothing is unread, the filter hides both works.
            vm.state.first { it.isFilteredOut }
            vm.setOptions(LibraryOptions())
            vm.search("p1-2")
            vm.state
                .first { it.items.size == 1 }
                .items
                .single()
                .manga.title shouldBe "Obra p1-2"
            track(LibraryViewModel(db.mangas, db.categories, db.history, store)).state.first { !it.loading }.options shouldBe
                LibraryOptions()
        }
}
