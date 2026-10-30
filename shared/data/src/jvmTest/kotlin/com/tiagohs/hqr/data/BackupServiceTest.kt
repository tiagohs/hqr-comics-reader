package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.backup.Backup
import com.tiagohs.hqr.data.backup.BackupException
import com.tiagohs.hqr.data.backup.BackupService
import com.tiagohs.hqr.data.backup.gunzip
import com.tiagohs.hqr.data.backup.gzip
import com.tiagohs.hqr.data.db.FeedItemEntity
import com.tiagohs.hqr.data.db.SavedSearchEntity
import com.tiagohs.hqr.data.db.ServerEntity
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock

/** Backup and restore between two devices: a real database on each side (RF-72 to RF-74, RF-89). */
class BackupServiceTest {
    private val phone = Device()
    private val tablet = Device()

    private class Device {
        val db = inMemoryDatabase()
        val settings = MapSettings()
        val mangas = RoomMangaRepository(db, Clock.System)
        val chapters = RoomChapterRepository(db, Clock.System)
        val categories = RoomCategoryRepository(db)
        val history = RoomHistoryRepository(db, mangas, chapters)
        var known = mapOf("mangadex" to "MangaDex", "hqnow" to "HQ Now")
        var broken = false
        val backups = BackupService(db, settings, "HQR 1.0.0", { if (broken) error("quebrou") else known })
    }

    @AfterTest
    fun close() {
        phone.db.close()
        tablet.db.close()
    }

    private suspend fun Device.work(
        source: SourceId,
        key: String,
    ): Pair<Long, List<Long>> {
        val manga = mangas.upsert(SourceManga(source, key, "Obra $key"))
        val list = chapters.sync(manga.id, (1..6).map { SourceChapter("c$it", it.toDouble()) }).sortedBy { it.number }
        return manga.id to list.map { it.id }
    }

    @Test
    fun RF72_RF74_everythingTravelsExceptThePassword() =
        runTest {
            val (id, chs) = phone.work(SourceId.MANGADEX, "frieren")
            phone.mangas.setInLibrary(id, true)
            val lendo = phone.categories.create("Lendo")
            phone.categories.setCategories(id, setOf(lendo.id))
            phone.chapters.setRead(chs.take(3), true)
            phone.chapters.saveProgress(chs[3], 7, 20)
            phone.history.record(chs[3], 1_000)
            phone.db.serverDao().upsert(ServerEntity("opds-1", "opds", "Casa", "https://komga.casa/opds", "tiago", null))
            SettingsSecretStore(phone.settings).put("server.password.opds-1", "segredo")
            AppPreferences(phone.settings).setCoverColor(false)
            val file = phone.backups.create()
            gunzip(file).decodeToString() shouldNotContain "segredo"

            val summary = tablet.backups.restoreOrThrow(file)
            summary.works shouldBe 1
            summary.chaptersRead shouldBe 3
            summary.servers shouldBe 1
            val restored = tablet.mangas.find(SourceId.MANGADEX, "frieren")!!
            restored.inLibrary shouldBe true
            val list = tablet.chapters.list(restored.id).sortedBy { it.number }
            list.filter { it.read }.map { it.number } shouldContainExactly listOf(1.0, 2.0, 3.0)
            list.first { it.number == 4.0 }.lastPage shouldBe 7
            tablet.categories
                .observeFor(restored.id)
                .first()
                .size shouldBe 1
            AppPreferences(tablet.settings).coverColor.value shouldBe false
            tablet.db
                .serverDao()
                .list()
                .single()
                .username shouldBe "tiago"
        }

    @Test
    fun RF89_restoreJoinsAndNeverLowersProgress() =
        runTest {
            val (pid, pchs) = phone.work(SourceId.MANGADEX, "akira")
            phone.mangas.setInLibrary(pid, true)
            phone.chapters.setRead(listOf(pchs[4]), true)
            phone.chapters.saveProgress(pchs[1], 3, 20)
            val (tid, tchs) = tablet.work(SourceId.MANGADEX, "akira")
            tablet.chapters.saveProgress(tchs[1], 15, 20)
            tablet.chapters.setRead(listOf(tchs[0]), true)
            tablet.backups.restoreOrThrow(phone.backups.create())
            val list = tablet.chapters.list(tid).sortedBy { it.number }
            list.filter { it.read }.map { it.number } shouldContainExactly listOf(1.0, 5.0)
            list[1].lastPage shouldBe 15
            tablet.mangas.get(tid)!!.inLibrary shouldBe true
        }

    @Test
    fun RF73_worksOfASourceThisAppDoesntHaveAreListedByName() =
        runTest {
            val (id, _) = phone.work(SourceId.HQ_NOW, "2219")
            phone.mangas.setInLibrary(id, true)
            tablet.known = mapOf("mangadex" to "MangaDex")
            val summary = tablet.backups.restoreOrThrow(phone.backups.create())
            summary.unavailableSources shouldBe listOf("HQ Now")
            tablet.mangas.find(SourceId.HQ_NOW, "2219")!!.inLibrary shouldBe true
            // The name stays on this device for the "Fonte indisponível" badge.
            tablet.db
                .sourceDao()
                .list()
                .single { it.id == "hqnow" }
                .name shouldBe "HQ Now"
        }

    @Test
    fun RF72_schemaVersionIsAlwaysWritten() =
        runTest {
            gunzip(phone.backups.create()).decodeToString() shouldContain "\"schemaVersion\":${Backup.SCHEMA_VERSION}"
        }

    @Test
    fun RF72_RF24_RF25_savedSearchesAndFeedTravelByName() =
        runTest {
            val search = phone.db.searchDao()
            val id = search.insertSaved(SavedSearchEntity(sourceId = "mangadex", name = "Romance", query = "amor", filters = "{}"))
            search.insertFeed(FeedItemEntity(sourceId = "mangadex", savedSearchId = null, sortOrder = 1))
            search.insertFeed(FeedItemEntity(sourceId = "mangadex", savedSearchId = id, sortOrder = 2))
            val file = phone.backups.create()
            tablet.backups.restoreOrThrow(file)
            // Twice: nothing doubles.
            tablet.backups.restoreOrThrow(file)
            val saved = tablet.db.searchDao().listSaved()
            saved.map { it.name } shouldContainExactly listOf("Romance")
            tablet.db
                .searchDao()
                .listFeed()
                .map { it.savedSearchId } shouldContainExactly listOf(null, saved.single().id)
        }

    @Test
    fun RF89_failedRestoreChangesNothing() =
        runTest {
            val (id, chs) = phone.work(SourceId.MANGADEX, "akira")
            phone.mangas.setInLibrary(id, true)
            phone.chapters.setRead(chs.take(2), true)
            val file = phone.backups.create()
            tablet.broken = true
            val failure = tablet.backups.restore(file).exceptionOrNull() as com.tiagohs.hqr.domain.repository.RestoreFailure
            failure.problem shouldBe com.tiagohs.hqr.domain.repository.RestoreProblem.NOT_A_BACKUP
            tablet.mangas.find(SourceId.MANGADEX, "akira") shouldBe null
        }

    @Test
    fun RF74_notABackupOrANewerOneIsRefused() =
        runTest {
            shouldThrow<BackupException> { tablet.backups.restoreOrThrow("oi".encodeToByteArray()) }
            val newer = gzip("""{"schemaVersion":${Backup.SCHEMA_VERSION + 1},"createdAtEpochMs":0,"app":"x"}""".encodeToByteArray())
            shouldThrow<BackupException> { tablet.backups.restoreOrThrow(newer) }.newerVersion shouldBe true
            (tablet.backups.restore(newer).exceptionOrNull() as com.tiagohs.hqr.domain.repository.RestoreFailure).problem shouldBe
                com.tiagohs.hqr.domain.repository.RestoreProblem.NEWER_VERSION
            tablet.backups.fileName().endsWith(".hqrbackup") shouldBe true
        }
}
