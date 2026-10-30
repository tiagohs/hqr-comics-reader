package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class SourceConfigTest {
    private open class Stub(
        override val id: SourceId,
    ) : CatalogSource {
        override val name = id.value
        override val languages = listOf("pt-br")

        override suspend fun popular(page: Int) = MangaPage(emptyList(), false)

        override suspend fun latest(page: Int) = MangaPage(emptyList(), false)

        override suspend fun details(manga: SourceManga) = manga

        override suspend fun chapters(manga: SourceManga) = emptyList<SourceChapter>()

        override suspend fun pages(
            manga: SourceManga,
            chapter: SourceChapter,
        ) = emptyList<SourcePage>()
    }

    private var now = Instant.fromEpochMilliseconds(1_790_000_000_000)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val db = inMemoryDatabase()
    private val settings = MapSettings()
    private val registry = SourceRegistry(listOf(Stub(SourceId.MANGADEX), Stub(SourceId.HQ_NOW), Stub(SourceId.LOCAL)))
    private var remote: String? = null
    private var fetches = 0
    private val fetcher =
        SourceListFetcher {
            fetches++
            remote
        }

    private fun config(version: String = "1.0.0") = SourceConfig(db, settings, registry, fetcher, version, clock)

    @AfterTest
    fun close() = db.close()

    @Test
    fun RF105_embeddedListMatchesTheRepositoryFile() {
        val file = generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "sources.json") }.first { it.exists() }
        SourceConfig.EMBEDDED shouldBe file.readText()
    }

    @Test
    fun RF105_listTurnsASourceOffAndChangesItsAddress() =
        runTest {
            remote = """{"sources":[{"id":"hqnow","enabled":false},{"id":"mangadex","baseUrl":"https://new.example"}]}"""
            val config = config()
            config.start()
            registry.active.value.map { it.id } shouldBe listOf(SourceId.MANGADEX, SourceId.LOCAL)
            registry[SourceId.HQ_NOW] shouldBe null
            config.baseUrl(SourceId.MANGADEX) shouldBe "https://new.example"
        }

    @Test
    fun RF105_onceADayAndTheLastGoodListStays() =
        runTest {
            remote = """{"sources":[{"id":"hqnow","enabled":false}]}"""
            config().start()
            fetches shouldBe 1
            // Next opening the same day: no request, the saved list applies.
            remote = null
            registry.setListDisabled(emptySet())
            config().start()
            fetches shouldBe 1
            registry[SourceId.HQ_NOW] shouldBe null
            // A day later the list can't be read, or is broken: the last good one stays.
            now += 25.hours
            remote = "<html>"
            config().start()
            fetches shouldBe 2
            registry[SourceId.HQ_NOW] shouldBe null
        }

    @Test
    fun RF105_minimumAppVersion() =
        runTest {
            remote = """{"sources":[{"id":"mangadex","minAppVersion":"1.10.0"}]}"""
            config("1.2.0").start()
            registry[SourceId.MANGADEX] shouldBe null
            SourceConfig.olderThan("1.10.0", "1.2.0") shouldBe false
        }

    @Test
    fun RF03_userSwitchIsSavedAndRestored() =
        runTest {
            config().setEnabled(SourceId.HQ_NOW, false)
            registry.status(SourceId.HQ_NOW) shouldBe SourceRegistry.Status.OFF_BY_USER
            registry.setUserDisabled(emptySet())
            config().start()
            registry.active.value.map { it.id } shouldBe listOf(SourceId.MANGADEX, SourceId.LOCAL)
            config().setEnabled(SourceId.HQ_NOW, true)
            registry.status(SourceId.HQ_NOW) shouldBe SourceRegistry.Status.ON
        }

    /** HQ Now's keys went from "Slug" to "id:slug" (RF-91). */
    private class HqNowV2 : Stub(SourceId.HQ_NOW) {
        override val schemaVersion = 2

        override fun migrateMangaKey(
            key: String,
            fromVersion: Int,
        ) = if (fromVersion < 2) "id:${key.lowercase()}" else key

        override fun migrateChapterKey(
            key: String,
            fromVersion: Int,
        ) = if (fromVersion < 2) "ch:$key" else key
    }

    @Test
    fun RF91_keysMigrateOnceWhenTheSourceSchemaGrows() =
        runTest {
            config().applySaved()
            val mangas =
                com.tiagohs.hqr.data.repository
                    .RoomMangaRepository(db, clock)
            val chapters =
                com.tiagohs.hqr.data.repository
                    .RoomChapterRepository(db, clock)
            val saga = mangas.upsert(SourceManga(SourceId.HQ_NOW, "saga", "Saga"))
            chapters.sync(saga.id, listOf(SourceChapter("c1", 1.0)))
            // A key that would clash with one already saved stays as it was.
            mangas.upsert(SourceManga(SourceId.HQ_NOW, "x", "X"))
            mangas.upsert(SourceManga(SourceId.HQ_NOW, "X", "X maiúsculo"))
            val other = mangas.upsert(SourceManga(SourceId.MANGADEX, "saga", "Saga MD"))
            val updated = SourceRegistry(listOf(Stub(SourceId.MANGADEX), HqNowV2(), Stub(SourceId.LOCAL)))
            repeat(2) { SourceConfig(db, settings, updated, fetcher, "1.0.0", clock).applySaved() }
            mangas.get(saga.id)!!.source.key shouldBe "id:saga"
            chapters
                .observe(saga.id)
                .first()
                .single()
                .source.key shouldBe "ch:c1"
            // "x" and "X" both become "id:x": one moves, the other keeps its old key instead of failing the start.
            val keys =
                db
                    .sourceDao()
                    .mangaKeys(SourceId.HQ_NOW.value)
                    .map { it.key }
                    .toSet()
            keys.size shouldBe 3
            (keys - setOf("id:saga", "id:x")).single() shouldBe (if ("x" in keys) "x" else "X")
            mangas.get(other.id)!!.source.key shouldBe "saga"
            db
                .sourceDao()
                .list()
                .single { it.id == SourceId.HQ_NOW.value }
                .schemaVersion shouldBe 2
        }
}
