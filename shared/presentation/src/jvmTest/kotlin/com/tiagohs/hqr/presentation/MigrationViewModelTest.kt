package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.presentation.migration.WorkMigration
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** RF-69: moving a work to another source (SCR-OBR-002). */
class MigrationViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val old = FakeSource(SourceId("flix"))
    private val yomu = FakeSource(SourceId("yomu"))
    private val other = FakeSource(SourceId("other"))
    private val registry = SourceRegistry(listOf(old, yomu, other))
    private val removed = mutableListOf<Long>()
    private val downloads =
        object : Downloads {
            override fun observe() = MutableStateFlow(emptyList<DownloadItem>())

            override suspend fun enqueue(chapterIds: List<Long>) = Unit

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
    private val migration = WorkMigration(registry, db.mangas, db.chapters, db.history, db.categories, downloads)

    /** "Solo Leveling" with chapters 1 to 20, all read, in a category and in the history. */
    private suspend fun oldWork(): Long {
        old.chapterList = (1..20).map { SourceChapter("f$it", it.toDouble()) }
        val manga = db.mangas.upsert(SourceManga(old.id, "solo", "Solo Leveling"))
        db.chapters.sync(manga.id, old.chapterList)
        val chapters = db.chapters.list(manga.id)
        db.chapters.setRead(chapters.map { it.id }, read = true)
        db.history.record(chapters.maxBy { it.number }.id, 1_000)
        db.mangas.setInLibrary(manga.id, true)
        val shonen = db.categories.create("Shonen")
        db.categories.setCategories(manga.id, setOf(shonen.id))
        return manga.id
    }

    @Test
    fun RF69_readChaptersByNumberCategoriesAndHistoryGoToTheNewSource() =
        runTest {
            val id = oldWork()
            // The new source numbers chapters 1 to 25 with its own keys.
            yomu.chapterList = (1..25).map { SourceChapter("y$it", it.toDouble()) }
            val vm = track(MigrationViewModel(id, registry, db.mangas, migration))
            val ready = vm.state.first { s -> s.results.isNotEmpty() && s.results.values.none { it.loading } }
            ready.query shouldBe "Solo Leveling"
            // The work's own source is not offered.
            ready.sources.map { it.id } shouldContainExactly listOf(yomu.id, other.id)
            vm.toggleSource(other.id)
            vm.state.value.results.keys shouldContainExactly setOf(yomu.id)
            vm.pick(
                vm.state.value.results
                    .getValue(yomu.id)
                    .items
                    .first(),
            )
            vm.migrate(deleteDownloads = true)
            val done = vm.state.first { it.done != null }.done!!
            done.readCarried shouldBe 20
            val chapters = db.chapters.list(done.mangaId)
            chapters.filter { it.read }.map { it.number } shouldContainExactlyInAnyOrder (1..20).map { it.toDouble() }
            chapters.single { it.number == 21.0 }.read shouldBe false
            db.mangas.get(done.mangaId)!!.inLibrary shouldBe true
            db.mangas.get(id)!!.inLibrary shouldBe false
            db.categories
                .observeFor(done.mangaId)
                .first()
                .size shouldBe 1
            db.history
                .observe()
                .first()
                .single()
                .chapter.number shouldBe 20.0
            db.history
                .observe()
                .first()
                .single()
                .manga.id shouldBe done.mangaId
            removed.size shouldBe 20
        }

    @Test
    fun RF69_aFailingSourceLeavesTheOldWorkAsItWas() =
        runTest {
            val id = oldWork()
            val vm = track(MigrationViewModel(id, registry, db.mangas, migration))
            vm.state.first { s -> s.results.isNotEmpty() && s.results.values.none { it.loading } }
            vm.pick(
                vm.state.value.results
                    .getValue(yomu.id)
                    .items
                    .first(),
            )
            yomu.fail = SourceException.Kind.NETWORK
            vm.migrate(deleteDownloads = false)
            vm.state.first { it.error != null }.error shouldBe ErrorKind.NETWORK
            db.mangas.get(id)!!.inLibrary shouldBe true
            db.chapters.list(id).count { it.read } shouldBe 20
            removed.size shouldBe 0
        }
}
