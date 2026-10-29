package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.presentation.reader.ChapterPages
import com.tiagohs.hqr.presentation.reader.ReaderSettings
import com.tiagohs.hqr.presentation.reader.ReaderSettingsStore
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlin.test.Test

class ReaderAndHistoryViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val source = FakeSource()

    private class MemorySettings : ReaderSettingsStore {
        var global = ReaderSettings()
        val perManga = mutableMapOf<Long, ReaderSettings>()

        override suspend fun forManga(manga: Manga) = perManga[manga.id] ?: global

        override suspend fun saveForManga(
            mangaId: Long,
            settings: ReaderSettings,
        ) {
            perManga[mangaId] = settings
        }

        override suspend fun saveGlobal(settings: ReaderSettings) {
            global = settings
        }
    }

    private var pagesPerChapter = 10
    private var failPages: SourceException.Kind? = null
    private val pages =
        ChapterPages { _, chapter ->
            failPages?.let { throw SourceException("falhou", it) }
            (0 until pagesPerChapter).map { SourcePage(it, "https://img/${chapter.source.key}/$it.jpg") }
        }
    private val settings = MemorySettings()

    private suspend fun work(vararg numbers: Double): Pair<Long, List<Chapter>> {
        val manga = db.mangas.upsert(source.popular(1).items.first())
        db.chapters.sync(manga.id, numbers.map { SourceChapter("c$it", it) })
        return manga.id to db.chapters.list(manga.id).sortedBy { it.number }
    }

    private fun reader(
        mangaId: Long,
        chapterId: Long,
        startPage: Int = -1,
    ) = ReaderViewModel(mangaId, chapterId, startPage, db.mangas, db.chapters, db.history, settings, pages, fixedClock)

    /** Waits in real time for work done on Room's threads. */
    private suspend fun eventually(condition: suspend () -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { while (!condition()) delay(20) }
        }

    private suspend fun ReaderViewModel.loaded() = state.first { !it.loading }

    @Test
    fun RF43_resumesWherePausedAndSavesAfterTheDelay() =
        runTest {
            val (id, chapters) = work(1.0, 2.0)
            db.chapters.saveProgress(chapters[0].id, 6, 10)
            val vm = reader(id, chapters[0].id)
            vm.loaded().page shouldBe 6
            vm.onPageChanged(7)
            vm.onPageChanged(8)
            vm.state.value.page shouldBe 8
            advanceTimeBy(ReaderViewModel.SAVE_DELAY_MS + 100)
            eventually { db.chapters.get(chapters[0].id)?.lastPage == 8 }
        }

    @Test
    fun RF41_RF42_endShowsTheTransitionWithTheGap() =
        runTest {
            val (id, chapters) = work(10.0, 12.0)
            val vm = reader(id, chapters[0].id, 0)
            vm.loaded().next?.number shouldBe 12.0
            vm.onReachedEnd()
            val transition = vm.state.value.transition!!
            transition.missing shouldContainExactly listOf(11)
            eventually { db.chapters.get(chapters[0].id)?.read == true }
            vm.goToNext()
            vm.state
                .first { !it.loading && it.chapter?.number == 12.0 }
                .previous
                ?.number shouldBe 10.0
        }

    @Test
    fun RF59_openingRecordsHistory() =
        runTest {
            val (id, chapters) = work(1.0)
            reader(id, chapters[0].id, 0).loaded()
            val history = HistoryViewModel(db.history, fixedClock, TimeZone.UTC)
            val day =
                history.state
                    .first { !it.loading }
                    .days
                    .single()
            day.isToday shouldBe true
            day.entries
                .single()
                .chapter.id shouldBe chapters[0].id
            history.clearAll()
            history.state.first { it.isEmpty }
        }

    @Test
    fun RF05_pagesFailureShowsTheErrorAndRetryRecovers() =
        runTest {
            val (id, chapters) = work(1.0)
            failPages = SourceException.Kind.NETWORK
            val vm = reader(id, chapters[0].id, 0)
            vm.loaded().error shouldBe ErrorKind.NETWORK
            failPages = null
            vm.retry()
            vm.state.first { !it.loading && it.error == null }.pageCount shouldBe 10
        }

    @Test
    fun RF40_emptyChapterIsReportedAndMenuToggles() =
        runTest {
            val (id, chapters) = work(1.0)
            pagesPerChapter = 0
            val vm = reader(id, chapters[0].id, 0)
            vm.loaded().isEmptyChapter shouldBe true
            vm.toggleMenu()
            vm.state.value.menuVisible shouldBe true
            vm.hideMenu()
            vm.state.value.menuVisible shouldBe false
        }

    @Test
    fun RF37_settingsSavedForTheWorkOrGlobally() =
        runTest {
            val (id, chapters) = work(1.0)
            val vm = reader(id, chapters[0].id, 0)
            vm.loaded()
            val webtoon = ReaderSettings(mode = com.tiagohs.hqr.presentation.reader.ReaderMode.WEBTOON)
            vm.updateSettings(webtoon, onlyThisWork = true)
            settings.perManga[id] shouldBe webtoon
            vm.updateSettings(webtoon.copy(showPageNumber = false), onlyThisWork = false)
            settings.global.showPageNumber shouldBe false
            vm.onPageFailed(3)
            vm.state.value.failedPages shouldContainExactly setOf(3)
            vm.onPageRetry(3)
            vm.state.value.failedPages shouldBe emptySet()
        }
}
