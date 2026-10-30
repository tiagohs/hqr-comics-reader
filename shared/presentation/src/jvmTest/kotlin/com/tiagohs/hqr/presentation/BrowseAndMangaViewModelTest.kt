package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel.Listing
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BrowseAndMangaViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val source = FakeSource()
    private val registry = SourceRegistry(listOf(source))

    private suspend fun awaitIdle(vm: BrowseSourceViewModel) = vm.state.first { !it.loading && !it.loadingMore }

    @Test
    fun RF21_popularLoadsAndContinuesAtTheEnd() =
        runTest {
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            awaitIdle(vm).items.size shouldBe 2
            vm.loadMore()
            vm.state.first { it.items.size == 4 && !it.loadingMore }
            vm.loadMore()
            val last = vm.state.first { it.items.size == 6 && !it.loadingMore }
            last.items.size shouldBe 6
            last.hasNextPage shouldBe false
            vm.loadMore()
            source.requested shouldContainExactly listOf("p1", "p2", "p3")
        }

    @Test
    fun RF21_latestAndSearchAreSeparateListings() =
        runTest {
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            awaitIdle(vm)
            vm.show(Listing.LATEST)
            vm.state.first { !it.loading && it.items.firstOrNull()?.key == "l1-1" }
            vm.search("naruto")
            vm.state.first { !it.loading && it.items.firstOrNull()?.key == "s:naruto:1-1" }
        }

    @Test
    fun RF05_failureStaysInTheSourceWithItsKind() =
        runTest {
            source.fail = SourceException.Kind.NETWORK
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            awaitIdle(vm).error shouldBe ErrorKind.NETWORK
            source.fail = null
            vm.retry()
            vm.state.first { !it.loading && it.items.size == 2 }.error shouldBe null
        }

    @Test
    fun RF05_unknownSourceIsAnErrorNotACrash() =
        runTest {
            val vm =
                track(
                    BrowseSourceViewModel(
                        com.tiagohs.hqr.domain.model
                            .SourceId("gone"),
                        registry,
                        db.mangas,
                    ),
                )
            vm.state.first { !it.loading }.error shouldBe ErrorKind.UNAVAILABLE
            val manga = track(MangaViewModel(999, registry, db.mangas, db.chapters))
            manga.state.first { !it.refreshing && it.error != null }.error shouldBe ErrorKind.NOT_FOUND
        }

    @Test
    fun RF98_workTakesItsColorFromTheCoverOnce() =
        runTest {
            val work =
                source
                    .popular(1)
                    .items
                    .first()
                    .copy(coverUrl = "https://x/capa.jpg")
            val id = db.mangas.upsert(work).id
            var reads = 0
            val yellow = IntArray(64 * 64) { 0xFFFCF543.toInt() }
            val pixels =
                CoverPixels {
                    reads++
                    yellow
                }
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters, pixels))
            val color =
                vm.state
                    .first { it.manga?.coverColor != null }
                    .manga!!
                    .coverColor
            color shouldBe CoverColor.seed(yellow)
            track(MangaViewModel(id, registry, db.mangas, db.chapters, pixels)).state.first { it.manga != null }
            reads shouldBe 1
        }

    @Test
    fun RF26_inLibraryKeysAreMarked() =
        runTest {
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            val first = awaitIdle(vm).items.first()
            val id = vm.open(first)
            db.mangas.setInLibrary(id, true)
            vm.state.first { first.key in it.inLibrary }
        }

    @Test
    fun RF29_RF31_workLoadsDetailsChaptersAndAction() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            val state = vm.state.first { !it.refreshing && it.chapters.size == 3 }
            state.manga?.source?.description shouldBe "Sinopse de Obra p1-1"
            state.action
                .shouldBeInstanceOf<ReadAction.Start>()
                .chapter.number shouldBe 1.0
            state.chapters.map { it.number } shouldContainExactly listOf(3.0, 2.0, 1.0)
            vm.setOrder(MangaViewModel.Order.OLDEST_FIRST)
            vm.state
                .first { it.order == MangaViewModel.Order.OLDEST_FIRST }
                .chapters
                .map { it.number } shouldContainExactly
                listOf(1.0, 2.0, 3.0)
        }

    @Test
    fun RF32_markPreviousAsReadAndFilterUnread() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            val third =
                vm.state
                    .first { it.chapters.size == 3 }
                    .chapters
                    .first { it.number == 3.0 }
            vm.markPreviousAsRead(third)
            vm.state
                .first { s ->
                    s.chapters.count { it.read } == 2
                }.action
                .shouldBeInstanceOf<ReadAction.Continue>()
                .chapter.number shouldBe
                3.0
            vm.setFilter(MangaViewModel.ChapterFilter.UNREAD)
            vm.state
                .first { it.filter == MangaViewModel.ChapterFilter.UNREAD }
                .chapters
                .map { it.number } shouldContainExactly listOf(3.0)
        }

    @Test
    fun RF33_refreshFailureKeepsWhatWasThere() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            vm.state.first { it.chapters.size == 3 }
            source.fail = SourceException.Kind.SERVER
            vm.refresh()
            val state = vm.state.first { it.error != null && !it.refreshing }
            state.error shouldBe ErrorKind.SERVER
            state.chapters.size shouldBe 3
        }

    @Test
    fun RF52_libraryShowsWhatWasAddedAndContinueReading() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val manga = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            manga.state.first { it.chapters.size == 3 }
            manga.toggleLibrary()
            val ch = db.chapters.list(id).first { it.number == 1.0 }
            db.chapters.saveProgress(ch.id, 4, 22)
            db.history.record(ch.id, 10)
            val library = track(LibraryViewModel(db.mangas, db.categories, db.history))
            val state = library.state.first { it.items.isNotEmpty() && it.continueReading != null }
            state.tabs.first().count shouldBe 1
            state.continueReading?.chapter?.lastPage shouldBe 4
        }
}
