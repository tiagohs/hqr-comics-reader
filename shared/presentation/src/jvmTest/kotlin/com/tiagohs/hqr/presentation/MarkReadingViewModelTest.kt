package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.ReadingMark
import com.tiagohs.hqr.domain.model.Shelf
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.library.LibraryArea
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MarkReadingViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MarkReadingViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val source = FakeSource().apply { chapterList = (1..10).map { SourceChapter("c$it", it.toDouble(), title = "Título $it") } }
    private val registry = SourceRegistry(listOf(source))

    private suspend fun work(withChapters: Boolean): Long {
        val manga = db.mangas.upsert(source.popular(1).items.first())
        db.mangas.setInLibrary(manga.id, true)
        if (withChapters) db.chapters.sync(manga.id, source.chapterList)
        return manga.id
    }

    private fun vm(id: Long) = track(MarkReadingViewModel(id, db.mangas, db.chapters, registry))

    private suspend fun MarkReadingViewModel.ready() =
        state.first {
            it is MarkReadingViewModel.State.Ready
        } as MarkReadingViewModel.State.Ready

    @Test
    fun RF126_up_to_a_chapter_marks_the_ones_before_and_undo_puts_everything_back() =
        runTest {
            val id = work(withChapters = true)
            val chs = db.chapters.list(id).sortedBy { it.number }
            db.chapters.saveProgress(chs[7].id, 4, 20)
            val vm = vm(id)
            // The furthest chapter reached is where the sheet opens.
            vm.ready().index shouldBe 7
            vm.chooseUpTo(3)
            vm.ready().let {
                it.mark shouldBe ReadingMark.UpTo(chs[3].id)
                it.readCount shouldBe 4
            }
            val done = CompletableDeferred<MarkReadingViewModel.Saved>()
            vm.save { done.complete(it) }
            val saved = done.await()
            saved.readCount shouldBe 4
            saved.total shouldBe 10
            db.chapters
                .list(id)
                .filter { it.read }
                .map { it.number }
                .sorted() shouldContainExactly listOf(1.0, 2.0, 3.0, 4.0)
            // The chapter that had a page loses it.
            db.chapters
                .list(id)
                .first { it.number == 8.0 }
                .lastPage shouldBe 0
            vm.undo()
            waitFor {
                db.chapters
                    .list(id)
                    .first { it.number == 8.0 }
                    .lastPage == 4
            }
            db.chapters.list(id).none { it.read } shouldBe true
        }

    @Test
    fun RF126_a_work_without_chapters_asks_the_source_before_it_can_save() =
        runTest {
            val id = work(withChapters = false)
            val gate = CompletableDeferred<Unit>()
            source.chapterGate = gate
            val vm = vm(id)
            vm.state.first { it is MarkReadingViewModel.State.Loading }
            gate.complete(Unit)
            vm.ready().chapters.size shouldBe 10
            vm.choose(ReadingMark.All)
            val done = CompletableDeferred<MarkReadingViewModel.Saved>()
            vm.save { done.complete(it) }
            done.await().readCount shouldBe 10
            db.chapters.list(id).all { it.read } shouldBe true
        }

    @Test
    fun RF126_a_source_that_fails_shows_the_error_and_tries_again() =
        runTest {
            val id = work(withChapters = false)
            source.chaptersFail = SourceException.Kind.NETWORK
            val vm = vm(id)
            vm.state
                .first { it is MarkReadingViewModel.State.Failed }
                .shouldBeInstanceOf<MarkReadingViewModel.State.Failed>()
                .error shouldBe
                ErrorKind.NETWORK
            source.chaptersFail = null
            vm.retry()
            vm.ready().chapters.size shouldBe 10
        }

    @Test
    fun RF126_several_works_become_unread_and_undo_brings_them_back() =
        runTest {
            val a = work(withChapters = true)
            val b = db.mangas.upsert(source.popular(1).items[1]).id
            db.mangas.setInLibrary(b, true)
            db.chapters.sync(b, source.chapterList)
            db.chapters.setRead(db.chapters.list(a).map { it.id }, true)
            db.chapters.setRead(
                db.chapters
                    .list(b)
                    .take(2)
                    .map { it.id },
                true,
            )
            val library = track(LibraryViewModel(db.mangas, db.categories, db.history, chapters = db.chapters))
            library.state.first { it.items.size == 2 }
            library.toggleSelected(a)
            library.toggleSelected(b)
            val done = CompletableDeferred<LibraryViewModel.Marked>()
            library.markSelected(read = false) { done.complete(it) }
            val marked = done.await()
            marked.works shouldBe 2
            db.chapters.list(a).none { it.read } shouldBe true
            library.undoMark(marked)
            waitFor { db.chapters.list(a).all { it.read } && db.chapters.list(b).count { it.read } == 2 }
        }

    @Test
    fun RF128_the_library_shows_everything_or_only_comics_or_only_manga() =
        runTest {
            val a = work(withChapters = true)
            val b = db.mangas.upsert(source.popular(1).items[1]).id
            db.mangas.setInLibrary(b, true)
            val shelves = MutableStateFlow(mapOf("fake" to Shelf.MANGA))
            val library = track(LibraryViewModel(db.mangas, db.categories, db.history, sourceShelves = shelves))
            library.state.first { it.items.size == 2 }.areaCounts shouldBe
                mapOf(LibraryArea.ALL to 2, LibraryArea.COMICS to 0, LibraryArea.MANGA to 2)
            db.mangas.setShelf(b, Shelf.COMICS)
            library.selectArea(LibraryArea.COMICS)
            library.state.first { s -> s.area == LibraryArea.COMICS && s.items.map { it.manga.id } == listOf(b) }
            shelves.value = mapOf("fake" to Shelf.COMICS)
            library.state.first { it.items.size == 2 }
            library.selectArea(LibraryArea.MANGA)
            library.state.first { it.area == LibraryArea.MANGA }.let {
                it.items shouldBe emptyList()
                // An empty area is not an empty library.
                it.isEmpty shouldBe false
            }
            db.mangas.get(a)!!.shelf shouldBe null
        }
}
