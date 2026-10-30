package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.SourceChapter
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
    fun RF22_filtersComeFromTheSourceAndGoWithTheSearch() =
        runTest {
            val romance =
                com.tiagohs.hqr.domain.source.SourceFilter
                    .Option("r", "Romance")
            val horror =
                com.tiagohs.hqr.domain.source.SourceFilter
                    .Option("h", "Horror")
            source.offeredFilters =
                listOf(
                    com.tiagohs.hqr.domain.source.SourceFilter
                        .Tags("tags", listOf(romance, horror)),
                )
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            val loaded = vm.state.first { it.filters.isNotEmpty() && !it.loading }
            loaded.activeFilters shouldBe 0
            val chosen =
                loaded.filters.map {
                    (it as com.tiagohs.hqr.domain.source.SourceFilter.Tags).copy(
                        included = setOf("r"),
                        excluded = setOf("h"),
                    )
                }
            vm.applyFilters(chosen)
            val searched = vm.state.first { it.listing == Listing.SEARCH && !it.loading }
            searched.activeFilters shouldBe 1
            source.lastFilters shouldBe chosen
            vm.defaultFilters() shouldBe source.offeredFilters
        }

    @Test
    fun RF23_openedFromGlobalSearchStartsOnThatSearch() =
        runTest {
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas, initialQuery = "akira"))
            vm.state.first { !it.loading }.let {
                it.listing shouldBe Listing.SEARCH
                it.items.first().key shouldBe "s:akira:1-1"
            }
            source.requested.none { it.startsWith("p") } shouldBe true
        }

    @Test
    fun RF21_loadMoreFailureKeepsTheItemsAndOffersRetry() =
        runTest {
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas))
            awaitIdle(vm).items.size shouldBe 2
            source.fail = SourceException.Kind.NETWORK
            vm.loadMore()
            val failed = vm.state.first { it.loadMoreError != null }
            failed.items.size shouldBe 2
            failed.error shouldBe null
            source.fail = null
            vm.loadMore()
            vm.state.first { it.items.size == 4 && it.loadMoreError == null }
        }

    @Test
    fun RF88_sourceScreenRestoresListingQueryAndFilters() =
        runTest {
            val tags =
                com.tiagohs.hqr.domain.source.SourceFilter.Tags(
                    "tags",
                    listOf(
                        com.tiagohs.hqr.domain.source.SourceFilter
                            .Option("r", "Romance"),
                    ),
                )
            source.offeredFilters = listOf(tags)
            val saved = androidx.lifecycle.SavedStateHandle()
            val vm = track(BrowseSourceViewModel(source.id, registry, db.mangas, saved = saved))
            vm.state.first { it.filters.isNotEmpty() && !it.loading }
            vm.search("frieren", listOf(tags.copy(included = setOf("r"))))
            vm.state.first { it.listing == Listing.SEARCH && !it.loading }
            val again = track(BrowseSourceViewModel(source.id, registry, db.mangas, saved = saved))
            again.state.first { !it.loading }.let {
                it.listing shouldBe Listing.SEARCH
                it.query shouldBe "frieren"
                (it.filters.single() as com.tiagohs.hqr.domain.source.SourceFilter.Tags).included shouldBe setOf("r")
                it.items.first().key shouldBe "s:frieren:1-1"
            }
        }

    @Test
    fun RF52_removedFromTheWorkScreenComesBackWithUndo() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            db.mangas.setInLibrary(id, true)
            val manga = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            manga.toggleLibrary()
            val removal = manga.removed.first()
            db.mangas.get(id)!!.inLibrary shouldBe false
            manga.undoRemove(removal)
            manga.state.first { it.manga?.inLibrary == true }
        }

    @Test
    fun RF62_downloadOneSelectedNextUnreadAndDelete() =
        runTest {
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val queued = mutableListOf<List<Long>>()
            val removed = mutableListOf<Long>()
            val downloads =
                object : com.tiagohs.hqr.domain.repository.Downloads {
                    override fun observe() = kotlinx.coroutines.flow.flowOf(emptyList<com.tiagohs.hqr.domain.repository.DownloadItem>())

                    override suspend fun enqueue(chapterIds: List<Long>) {
                        queued += chapterIds
                    }

                    override suspend fun pauseAll() = Unit

                    override suspend fun resumeAll() = Unit

                    override suspend fun move(
                        chapterId: Long,
                        by: Int,
                    ) = Unit

                    override suspend fun remove(chapterIds: Collection<Long>) {
                        removed += chapterIds
                    }

                    override suspend fun removeAll() = Unit

                    override suspend fun usedBytes() = 0L
                }
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters, downloads = downloads))
            val chapters =
                vm.state
                    .first { it.chapters.size == 3 }
                    .chapters
                    .sortedBy { it.number }
            vm.canDownload shouldBe true
            db.chapters.setRead(listOf(chapters[0].id), true)
            vm.downloadUnread(1)
            waitFor { queued.size == 1 }
            queued[0] shouldContainExactly listOf(chapters[1].id)
            vm.toggleSelected(chapters[2].id)
            vm.toggleSelected(chapters[0].id)
            vm.selected.value.size shouldBe 2
            vm.download(vm.selected.value)
            waitFor { queued.size == 2 }
            queued[1] shouldContainExactly listOf(chapters[0].id, chapters[2].id)
            vm.selected.value shouldBe emptySet()
            vm.downloadUnread()
            waitFor { queued.size == 3 }
            queued[2] shouldContainExactly listOf(chapters[1].id, chapters[2].id)
            vm.removeDownloads(listOf(chapters[1].id))
            waitFor { removed.isNotEmpty() }
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
    fun RF98_newCoverDuringOpeningKeepsTheNewColor() =
        runTest {
            val work =
                source
                    .popular(1)
                    .items
                    .first()
                    .copy(coverUrl = "https://x/old.jpg")
            val id = db.mangas.upsert(work).id
            // The source answers the refresh with a new cover; the pixels read are those of the new one.
            source.detailsCover = "https://x/new.jpg"
            val seen = mutableListOf<String?>()
            val green = IntArray(64 * 64) { 0xFF1B8A5A.toInt() }
            val pixels =
                CoverPixels { manga ->
                    seen += manga.source.coverUrl
                    green
                }
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters, pixels))
            vm.state
                .first { it.manga?.coverColor != null }
                .manga!!
                .source.coverUrl shouldBe "https://x/new.jpg"
            seen shouldContainExactly listOf("https://x/new.jpg")
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
    fun RF30_filterByGroup() =
        runTest {
            source.chapterList =
                listOf(
                    SourceChapter("a", 1.0, group = "Aurora"),
                    SourceChapter("b", 1.0, group = "Kitsune"),
                    SourceChapter("c", 2.0, group = "Aurora"),
                )
            val id = db.mangas.upsert(source.popular(1).items.first()).id
            val vm = track(MangaViewModel(id, registry, db.mangas, db.chapters))
            vm.state.first { it.chapters.size == 3 }.groups shouldContainExactly listOf("Aurora", "Kitsune")
            vm.setGroup("Kitsune")
            vm.state
                .first { it.group == "Kitsune" }
                .chapters
                .map { it.source.key } shouldContainExactly listOf("b")
            vm.setGroup(null)
            vm.state.first { it.group == null && it.chapters.size == 3 }
        }

    @Test
    fun RF33_chaptersFoundAfterTheLastOpeningAreNew() =
        runTest {
            var now = 1_000L
            val ticking =
                object : kotlin.time.Clock {
                    override fun now() = kotlin.time.Instant.fromEpochMilliseconds(now++)
                }
            val tdb = TestDb(ticking)
            val id = tdb.mangas.upsert(source.popular(1).items.first()).id
            val first = track(MangaViewModel(id, registry, tdb.mangas, tdb.chapters))
            first.state.first { it.chapters.size == 3 && !it.refreshing }.newChapterIds shouldBe emptySet()
            source.chapterList = source.chapterList + SourceChapter("c4", 4.0)
            val second = track(MangaViewModel(id, registry, tdb.mangas, tdb.chapters))
            val state = second.state.first { it.chapters.size == 4 && !it.refreshing && it.newChapterIds.isNotEmpty() }
            state.chapters.filter { it.id in state.newChapterIds }.map { it.number } shouldContainExactly listOf(4.0)
            // Opened again: nothing is new any more.
            track(MangaViewModel(id, registry, tdb.mangas, tdb.chapters))
                .state
                .first { it.chapters.size == 4 && !it.refreshing }
                .newChapterIds shouldBe emptySet()
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
