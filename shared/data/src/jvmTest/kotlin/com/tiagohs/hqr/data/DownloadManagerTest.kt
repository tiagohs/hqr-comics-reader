package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.download.DownloadFiles
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock

/** The download queue over a real database and a fake disk (RF-62 to RF-66). */
class DownloadManagerTest {
    private val db = inMemoryDatabase()
    private val fs = FakeFileSystem()
    private val files = DownloadFiles(fs, "/data/downloads".toPath())
    private val mangas = RoomMangaRepository(db, Clock.System)
    private val chapters = RoomChapterRepository(db, Clock.System)
    private var scheduled = 0
    private val restarts = mutableListOf<Boolean>()
    private var deleteAfterRead = false
    private val empty = mutableSetOf<Long>()
    private val asked = mutableListOf<String>()
    private var failOn: Int? = null
    private var onPage: suspend (Long, Int) -> Unit = { _, _ -> }
    private val manager =
        DownloadManager(
            db,
            files,
            DownloadScheduler { restart ->
                scheduled++
                restarts += restart
            },
            pagesOf = { id -> if (id in empty) emptyList() else (0 until 3).map { SourcePage(it, "https://img/$id/$it") } },
            bytesOf = { id, page ->
                onPage(id, page.index)
                if (page.index == failOn) error("falhou")
                asked += page.imageUrl
                "p${page.index}".encodeToByteArray()
            },
            deleteAfterRead = { deleteAfterRead },
        )

    @AfterTest
    fun close() = db.close()

    private suspend fun work(): List<Long> {
        val manga = mangas.upsert(SourceManga(SourceId.MANGADEX, "w", "Obra"))
        return chapters.sync(manga.id, (1..4).map { SourceChapter("c$it", it.toDouble()) }).sortedBy { it.number }.map { it.id }
    }

    @Test
    fun RF62_RF65_queueDownloadsInOrderAndTheReaderGetsTheFiles() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(2))
            scheduled shouldBe 1
            manager.observe().first().map { it.chapterLabel } shouldContainExactly listOf("1", "2")
            manager.run { true }
            manager.observe().first().map { it.state } shouldBe listOf(DownloadState.DONE, DownloadState.DONE)
            val pages = manager.offlinePages(ids[0])!!
            pages.size shouldBe 3
            files.read(files.pathOf(pages[1].imageUrl)!!).decodeToString() shouldBe "p1"
            manager.offlinePages(ids[2]) shouldBe null
            manager.usedBytes() shouldBe 12
        }

    @Test
    fun RF64_onlyOnWifiWaits() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(1))
            manager.run { false }
            manager
                .observe()
                .first()
                .single()
                .state shouldBe DownloadState.WAITING_WIFI
            asked shouldBe emptyList()
            manager.run { true }
            manager
                .observe()
                .first()
                .single()
                .state shouldBe DownloadState.DONE
        }

    @Test
    fun RF63_pausedChapterKeepsItsPagesAndContinuesFromThem() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(1))
            onPage = { _, index -> if (index == 1) manager.pauseAll() }
            manager.run { true }
            val paused = manager.observe().first().single()
            paused.state shouldBe DownloadState.PAUSED
            onPage = { _, _ -> }
            asked.clear()
            manager.resumeAll()
            manager.run { true }
            // Pages 0 and 1 were saved before the pause took effect: only 2 is asked again.
            asked shouldContainExactly listOf("https://img/${ids[0]}/2")
            manager
                .observe()
                .first()
                .single()
                .state shouldBe DownloadState.DONE
        }

    @Test
    fun RF63_failureIsShownAndTheQueueGoesOn() =
        runTest {
            val ids = work()
            failOn = 2
            manager.enqueue(ids.take(2))
            failOn = null
            failOn = 2
            onPage = { id, _ -> if (id == ids[1]) failOn = null }
            manager.run { true }
            manager.observe().first().map { it.state } shouldBe listOf(DownloadState.ERROR, DownloadState.DONE)
            manager.observe().first()[0].error shouldBe "falhou"
            manager.resumeAll()
            manager.run { true }
            manager.observe().first().map { it.state } shouldBe listOf(DownloadState.DONE, DownloadState.DONE)
        }

    @Test
    fun RF63_reorderAndRemove() =
        runTest {
            val ids = work()
            manager.enqueue(ids)
            manager.move(ids[2], -2)
            manager.observe().first().map { it.chapterId } shouldContainExactly listOf(ids[2], ids[0], ids[1], ids[3])
            manager.remove(listOf(ids[0]))
            manager.observe().first().map { it.chapterId } shouldContainExactly listOf(ids[2], ids[1], ids[3])
            manager.run { true }
            manager.removeAll()
            manager.observe().first() shouldBe emptyList()
            manager.usedBytes() shouldBe 0
        }

    @Test
    fun RF66_deleteAfterReadOnlyWhenAChapterBecomesRead() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(3))
            manager.run { true }
            chapters.setRead(listOf(ids[0]), read = true)
            // Option off: nothing goes.
            manager.chaptersRead(listOf(ids[0]))
            manager.offlinePages(ids[0]) shouldNotBe null
            deleteAfterRead = true
            chapters.setRead(listOf(ids[1]), read = true)
            manager.chaptersRead(listOf(ids[0], ids[1], ids[2]))
            // The read ones go; the unread one stays.
            manager.observe().first().map { it.chapterId } shouldContainExactly listOf(ids[2])
            fs.exists(files.dir(ids[0])) shouldBe false
            // A read chapter downloaded again stays until it is read again.
            manager.enqueue(listOf(ids[0]))
            manager.run { true }
            manager.offlinePages(ids[0]) shouldNotBe null
            files.pathOf("download:/etc/passwd") shouldBe null
        }

    @Test
    fun RF63_networkLossWaitsAndAsksForAnotherRun() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(2))
            onPage = { _, _ -> throw SourceException("sem rede", SourceException.Kind.NETWORK) }
            manager.run { true } shouldBe true
            manager.observe().first().map { it.state } shouldBe listOf(DownloadState.WAITING_WIFI, DownloadState.WAITING_WIFI)
            onPage = { _, _ -> }
            manager.run { true } shouldBe false
            manager.observe().first().map { it.state } shouldBe listOf(DownloadState.DONE, DownloadState.DONE)
        }

    @Test
    fun RF64_changingTheRuleRequeuesTheWaitingAndRestarts() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(1))
            manager.run { false } shouldBe true
            restarts.clear()
            manager.networkRuleChanged()
            manager
                .observe()
                .first()
                .single()
                .state shouldBe DownloadState.QUEUED
            restarts shouldContainExactly listOf(true)
        }

    @Test
    fun RF63_openingTheAppStartsAQueueLeftWithWork() =
        runTest {
            manager.resumePending()
            restarts shouldBe emptyList()
            val ids = work()
            manager.enqueue(ids.take(1))
            manager.pauseAll()
            restarts.clear()
            manager.resumePending()
            restarts shouldBe emptyList()
            manager.resumeAll()
            restarts.clear()
            manager.resumePending()
            restarts shouldContainExactly listOf(false)
        }

    @Test
    fun RF63_removedWhileDownloadingLeavesNoFiles() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(1))
            onPage = { id, index -> if (index == 1) manager.remove(listOf(id)) }
            manager.run { true }
            manager.observe().first() shouldBe emptyList()
            fs.exists(files.dir(ids[0])) shouldBe false
        }

    @Test
    fun RF63_stoppedRunPutsTheChapterBackInTheQueue() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(1))
            val started = CompletableDeferred<Unit>()
            onPage = { _, index ->
                if (index == 1) {
                    started.complete(Unit)
                    awaitCancellation()
                }
            }
            val job = launch { manager.run { true } }
            started.await()
            job.cancelAndJoin()
            val item = manager.observe().first().single()
            item.state shouldBe DownloadState.QUEUED
            item.pagesDone shouldBe 1
        }

    @Test
    fun RF63_moveWhileDownloadingKeepsTheWorkersProgress() =
        runTest {
            val ids = work()
            manager.enqueue(ids.take(3))
            onPage = { id, index -> if (id == ids[0] && index == 2) manager.move(ids[2], -1) }
            manager.run { true }
            val items = manager.observe().first()
            items.map { it.chapterId } shouldContainExactly listOf(ids[0], ids[2], ids[1])
            items.map { it.state }.distinct() shouldContainExactly listOf(DownloadState.DONE)
            items.map { it.pagesDone }.distinct() shouldContainExactly listOf(3)
        }

    @Test
    fun RF63_chapterWithoutPagesIsAnErrorNotADownload() =
        runTest {
            val ids = work()
            empty += ids[0]
            manager.enqueue(ids.take(1))
            manager.run { true }
            val item = manager.observe().first().single()
            item.state shouldBe DownloadState.ERROR
            manager.offlinePages(ids[0]) shouldBe null
        }
}
