package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/** Integration: the repositories over a real SQLite (bundled driver), in memory. */
class LibraryRepositoriesTest {
    private val clock =
        object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(1_790_000_000_000)
        }
    private val db: HqrDatabase = inMemoryDatabase()
    private val mangas = RoomMangaRepository(db, clock)
    private val chapters = RoomChapterRepository(db, clock)
    private val categories = RoomCategoryRepository(db)
    private val history = RoomHistoryRepository(db, mangas, chapters)

    @AfterTest
    fun close() = db.close()

    private val naruto =
        SourceManga(
            sourceId = SourceId.MANGADEX,
            key = "abc",
            title = "Naruto",
            authors = listOf("Masashi Kishimoto"),
            status = PublicationStatus.COMPLETED,
            description = "Ninja",
        )

    private fun chapter(
        n: Double,
        group: String = "Scan",
    ) = SourceChapter(key = "c$n-$group", number = n, group = group)

    @Test
    fun RF90_workIsIdentifiedBySourceAndKey() =
        runTest {
            val first = mangas.upsert(naruto)
            val again = mangas.upsert(naruto.copy(title = "Naruto (novo)"))
            again.id shouldBe first.id
            again.title shouldBe "Naruto (novo)"
            mangas.find(SourceId.MANGADEX, "abc")?.id shouldBe first.id
        }

    @Test
    fun RF29_listingDoesNotEraseWhatDetailsSaved() =
        runTest {
            mangas.upsert(naruto)
            val fromListing = mangas.upsert(SourceManga(SourceId.MANGADEX, "abc", "Naruto"))
            fromListing.source.description shouldBe "Ninja"
            fromListing.source.authors shouldContainExactly listOf("Masashi Kishimoto")
        }

    @Test
    fun RF33_RF92_syncKeepsProgressAndMarksRemoved() =
        runTest {
            val manga = mangas.upsert(naruto)
            val first = chapters.sync(manga.id, listOf(chapter(1.0), chapter(2.0)))
            val one = first.first { it.number == 1.0 }
            chapters.saveProgress(one.id, page = 5, pageCount = 20)
            val second = chapters.sync(manga.id, listOf(chapter(2.0), chapter(3.0)))
            val kept = second.first { it.number == 1.0 }
            kept.removedAtSource shouldBe true
            kept.lastPage shouldBe 5
            second.map { it.number }.sortedDescending() shouldContainExactly listOf(3.0, 2.0, 1.0)
        }

    @Test
    fun RF43_lastPageMarksTheChapterRead() =
        runTest {
            val manga = mangas.upsert(naruto)
            val c = chapters.sync(manga.id, listOf(chapter(1.0))).single()
            chapters.saveProgress(c.id, page = 7, pageCount = 22)
            chapters.get(c.id)?.read shouldBe false
            chapters.saveProgress(c.id, page = 21, pageCount = 22)
            chapters.get(c.id)?.read shouldBe true
        }

    @Test
    fun RF32_markSeveralAsReadAndUnread() =
        runTest {
            val manga = mangas.upsert(naruto)
            val list = chapters.sync(manga.id, listOf(chapter(1.0), chapter(2.0), chapter(3.0)))
            chapters.setRead(list.filter { it.number < 3.0 }.map { it.id }, read = true)
            chapters
                .list(manga.id)
                .filter { it.read }
                .map { it.number }
                .sorted() shouldContainExactly listOf(1.0, 2.0)
            chapters.setRead(list.map { it.id }, read = false)
            chapters.list(manga.id).none { it.read } shouldBe true
        }

    @Test
    fun RF52_RF54_libraryCountsUnreadByNumber() =
        runTest {
            val manga = mangas.upsert(naruto)
            val list = chapters.sync(manga.id, listOf(chapter(1.0, "A"), chapter(1.0, "B"), chapter(2.0)))
            chapters.setRead(list.filter { it.number == 1.0 }.map { it.id }, read = true)
            mangas.setInLibrary(manga.id, true)
            val item = mangas.observeLibrary().first().single()
            item.unread shouldBe 1
            item.totalChapters shouldBe 2
            item.manga.addedAtEpochMs shouldBe clock.now().toEpochMilliseconds()
            mangas.setInLibrary(manga.id, false)
            mangas.observeLibrary().first() shouldBe emptyList()
        }

    @Test
    fun RF53_categoriesCreateRenameReorderDelete() =
        runTest {
            val manga = mangas.upsert(naruto)
            mangas.setInLibrary(manga.id, true)
            val lendo = categories.create("Lendo")
            val depois = categories.create("Para depois")
            categories.setCategories(manga.id, setOf(lendo.id, depois.id))
            mangas
                .observeLibrary()
                .first()
                .single()
                .categoryIds shouldBe setOf(lendo.id, depois.id)
            categories.rename(lendo.id, "Lendo agora")
            categories.reorder(listOf(depois.id, lendo.id))
            categories.observe().first().map { it.name } shouldContainExactly listOf("Para depois", "Lendo agora")
            categories.delete(depois.id)
            mangas
                .observeLibrary()
                .first()
                .single()
                .categoryIds shouldBe setOf(lendo.id)
        }

    @Test
    fun RF59_RF60_historyRecordsRemovesAndClears() =
        runTest {
            val manga = mangas.upsert(naruto)
            val list = chapters.sync(manga.id, listOf(chapter(1.0), chapter(2.0)))
            history.record(list[0].id, 1000)
            history.record(list[1].id, 2000)
            history.observe().first().map { it.readAtEpochMs } shouldContainExactly listOf(2000L, 1000L)
            history
                .observeLatestPerManga()
                .first()
                .single()
                .readAtEpochMs shouldBe 2000L
            history.remove(list[1].id)
            history.observe().first().size shouldBe 1
            history.clear()
            history.observe().first() shouldBe emptyList()
        }

    @Test
    fun RF90_parallelSavesOfTheSameWorkKeepOneRow() =
        runTest {
            val work = SourceManga(SourceId.MANGADEX, "same", "Obra")
            val ids =
                kotlinx.coroutines.coroutineScope {
                    (1..20).map { async(kotlinx.coroutines.Dispatchers.Default) { mangas.upsert(work).id } }.awaitAll()
                }
            ids.toSet().size shouldBe 1
        }

    @Test
    fun RF98_coverColorIsKeptUntilTheCoverChanges() =
        runTest {
            val work = SourceManga(SourceId.MANGADEX, "c", "Obra", coverUrl = "https://x/1.jpg")
            val id = mangas.upsert(work).id
            mangas.setCoverColor(id, 0x123456)
            mangas.upsert(work.copy(title = "Obra 2")).coverColor shouldBe 0x123456
            mangas.upsert(work.copy(coverUrl = "https://x/2.jpg")).coverColor shouldBe null
        }
}
