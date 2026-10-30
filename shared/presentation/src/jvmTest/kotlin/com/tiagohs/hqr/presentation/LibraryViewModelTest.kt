package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryOptionsStore
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.library.TriState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            val removed = kotlinx.coroutines.CompletableDeferred<com.tiagohs.hqr.domain.repository.LibraryRemoval>()
            val addedAt = db.mangas.get(a)!!.addedAtEpochMs
            vm.remove(item) { removed.complete(it) }
            vm.state.first { it.items.size == 1 }
            // RF-53: a work out of the library leaves its categories.
            db.categories.observeFor(a).first() shouldBe emptySet()
            vm.undoRemove(removed.await())
            // The library joins several tables: wait until the categories arrive too.
            vm.state.first { s -> s.items.firstOrNull { it.manga.id == a }?.categoryIds == setOf(reading.id, done.id) }
            // "Desfazer" keeps the place under "Data em que entrou".
            db.mangas.get(a)!!.addedAtEpochMs shouldBe addedAt
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
            browse.toggleLibrary(first) shouldBe null
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
            val removal = browse.toggleLibrary(first)!!
            removal.categoryIds shouldBe setOf(a.id, b.id)
            db.categories.observeFor(id).first() shouldBe emptySet()
            // "Desfazer" puts it back as it was, never toggles a second time.
            browse.undoToggle(first, removal)
            db.mangas.get(id)!!.inLibrary shouldBe true
            db.categories.observeFor(id).first() shouldBe setOf(a.id, b.id)
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
    fun RF88_tabAndSearchSurviveTheProcess() =
        runTest {
            addWorks()
            val cat = db.categories.create("Lendo")
            val saved = androidx.lifecycle.SavedStateHandle()
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history, saved = saved))
            vm.state.first { it.items.size == 2 }
            vm.selectTab(cat.id)
            vm.search("p1-2")
            vm.state.first { it.selectedTab == cat.id && it.query == "p1-2" }
            val again = track(LibraryViewModel(db.mangas, db.categories, db.history, saved = saved))
            again.state.first { !it.loading }.let {
                it.selectedTab shouldBe cat.id
                it.query shouldBe "p1-2"
            }
        }

    @Test
    fun RF55_filterOnADeletedCategoryIsDropped() =
        runTest {
            addWorks()
            val cat = db.categories.create("Some")
            val store = MemoryOptions()
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history, store))
            vm.setOptions(LibraryOptions(categories = setOf(cat.id)))
            vm.state.first { it.isFilteredOut }
            db.categories.delete(cat.id)
            // Never an empty library hidden by a filter the user can no longer see.
            vm.state
                .first { it.items.size == 2 }
                .options.categories shouldBe emptySet()
        }

    @Test
    fun RF53_categoriesScreenCountsWorks() =
        runTest {
            val (a, b) = addWorks()
            val cat = db.categories.create("Lendo")
            db.categories.setCategories(a, setOf(cat.id))
            val vm = track(CategoriesViewModel(db.categories, db.mangas))
            vm.state.first { it.counts[cat.id] == 1 }
            db.categories.setCategories(b, setOf(cat.id))
            vm.state.first { it.counts[cat.id] == 2 }
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

    @Test
    fun RF57_pickedWorksMoveMarkReadAndLeaveWithUndo() =
        runTest {
            val (a, b) = addWorks()
            val done = db.categories.create("Concluídas")
            listOf(a, b).forEach { id -> db.chapters.sync(id, source.chapterList) }
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history, chapters = db.chapters))
            vm.state.first { it.items.size == 2 }
            vm.toggleSelected(a)
            vm.selectAll()
            vm.picked.value shouldBe setOf(a, b)
            vm.moveSelected(setOf(done.id))
            vm.picked.value shouldBe emptySet()
            vm.state.first { s -> s.items.all { it.categoryIds == setOf(done.id) } }
            vm.toggleSelected(b)
            vm.markSelectedRead()
            waitFor { db.chapters.list(b).all { it.read } }
            db.chapters.list(a).none { it.read } shouldBe true
            vm.selectAll()
            val removed = kotlinx.coroutines.CompletableDeferred<List<com.tiagohs.hqr.domain.repository.LibraryRemoval>>()
            vm.removeSelected { removed.complete(it) }
            vm.state.first { it.isEmpty }
            vm.undoRemoveAll(removed.await())
            vm.state.first { s -> s.items.size == 2 && s.items.all { it.categoryIds == setOf(done.id) } }
        }

    @Test
    fun RF58_updateBringsNewChaptersOneWorkAfterAnotherAndCountsThem() =
        runTest {
            val (a, b) = addWorks()
            listOf(a, b).forEach { id -> db.chapters.sync(id, source.chapterList) }
            source.chapterList = source.chapterList +
                com.tiagohs.hqr.domain.model
                    .SourceChapter("c4", 4.0)
            val updater =
                com.tiagohs.hqr.presentation.library.LibraryUpdater(
                    com.tiagohs.hqr.domain.source
                        .SourceRegistry(listOf(source)),
                    db.mangas,
                    db.chapters,
                )
            val vm = track(LibraryViewModel(db.mangas, db.categories, db.history, chapters = db.chapters, updater = updater))
            vm.state.first { it.items.size == 2 }
            val seen = mutableListOf<Int>()
            val watch = backgroundScope.launch { vm.update.collect { p -> p?.let { seen += it.done } } }
            vm.updateLibrary()
            val result = vm.updated.first { it != null }!!
            result.newChapters shouldBe 2
            result.failed shouldBe 0
            db.chapters.list(a).size shouldBe 4
            (seen.isNotEmpty()) shouldBe true
            watch.cancel()
            vm.updateShown()
            vm.updated.value shouldBe null
        }

    @Test
    fun RF58_aFailingSourceIsCountedAndStoppingKeepsWhatArrived() =
        runTest {
            addWorks()
            source.fail = com.tiagohs.hqr.domain.source.SourceException.Kind.SERVER
            val updater =
                com.tiagohs.hqr.presentation.library.LibraryUpdater(
                    com.tiagohs.hqr.domain.source
                        .SourceRegistry(listOf(source)),
                    db.mangas,
                    db.chapters,
                )
            val works =
                db.mangas
                    .observeLibrary()
                    .first()
                    .map { it.manga }
            val result = updater.run(works) {}
            result.failed shouldBe 2
            result.newChapters shouldBe 0
        }
}
