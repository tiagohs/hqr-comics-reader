package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.data.sync.HubClient
import com.tiagohs.hqr.data.sync.SyncAccount
import com.tiagohs.hqr.data.sync.SyncLocalData
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.repository.SyncError
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/** Two devices with real databases and a stand-in for the Worker that stores and orders like it (RF-119 to RF-125). */
class SyncAccountTest {
    /** The Worker's rules (hub-worker/src/sync.ts): the newest version of each item, ordered by a per-account seq. */
    private class FakeHub {
        data class Row(
            val change: JsonObject,
            val changedAt: Long,
            val seq: Long,
        )

        val rows = LinkedHashMap<String, Row>()
        var seq = 0L
        var online = true
        var allowed = setOf("tiago@example.com")
        val sessions = mutableSetOf<String>()
        val bodies = mutableListOf<String>()

        val engine =
            MockEngine { request ->
                val json = headersOf("Content-Type", "application/json")
                if (!online) throw java.io.IOException("offline")
                val body = (request.body as TextContent).text
                bodies += body
                when (request.url.encodedPath) {
                    "/api/v1/auth/google" -> {
                        val email =
                            Json
                                .parseToJsonElement(body)
                                .jsonObject["idToken"]!!
                                .jsonPrimitive.content
                        if (email !in allowed) return@MockEngine respond("""{"error":"not_allowed"}""", HttpStatusCode.Forbidden, json)
                        val token = "token-${sessions.size}-abcdefghijklmnopqrstuvwxyz"
                        sessions += token
                        respond("""{"token":"$token","email":"$email","expiresAt":1}""", HttpStatusCode.OK, json)
                    }
                    "/api/v1/auth/signout" -> respond("""{"ok":true}""", HttpStatusCode.OK, json)
                    "/api/v1/sync" -> {
                        val auth = request.headers["Authorization"]?.removePrefix("Bearer ")
                        if (auth !in sessions) return@MockEngine respondError(HttpStatusCode.Unauthorized)
                        val parsed = Json.parseToJsonElement(body).jsonObject
                        val cursor = parsed["cursor"]!!.jsonPrimitive.long
                        parsed["changes"]!!.jsonArray.map { it.jsonObject }.forEach { c ->
                            val id = c["kind"]!!.jsonPrimitive.content + "|" + c["key"]!!.jsonPrimitive.content
                            val at = c["changedAt"]!!.jsonPrimitive.long
                            val stored = rows[id]
                            if (stored == null || stored.changedAt <= at) rows[id] = Row(c, at, ++seq)
                        }
                        val page = rows.values.filter { it.seq > cursor }.sortedBy { it.seq }
                        val out = JsonArray(page.map { JsonObject(it.change + ("seq" to JsonPrimitive(it.seq))) })
                        respond("""{"cursor":$seq,"changes":$out,"more":false}""", HttpStatusCode.OK, json)
                    }
                    else -> respondError(HttpStatusCode.NotFound)
                }
            }
    }

    private class TestClock(
        var ms: Long = 1_000_000,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
    }

    private class Device(
        hub: FakeHub,
        val clock: TestClock,
        name: String,
    ) {
        val db = inMemoryDatabase()
        val settings = MapSettings()
        val prefs = AppPreferences(settings)
        val mangas = RoomMangaRepository(db, clock)
        val chapters = RoomChapterRepository(db, clock)
        val categories = RoomCategoryRepository(db)
        val history = RoomHistoryRepository(db, mangas, chapters)
        val account =
            SyncAccount(
                db,
                SyncLocalData(db, settings, prefs),
                HubClient(HttpClient(hub.engine), "https://hqr.example.workers.dev"),
                SettingsSecretStore(settings),
                settings,
                name,
                clock,
            )

        suspend fun work(
            key: String,
            inLibrary: Boolean = true,
        ): Pair<Long, List<Long>> {
            val manga = mangas.upsert(SourceManga(SourceId.MANGADEX, key, "Obra $key"))
            if (inLibrary) mangas.setInLibrary(manga.id, true)
            val list = chapters.sync(manga.id, (1..12).map { SourceChapter("c$it", it.toDouble()) }).sortedBy { it.number }
            return manga.id to list.map { it.id }
        }

        suspend fun libraryKeys() =
            db
                .mangaDao()
                .all()
                .filter { it.inLibrary }
                .map { it.key }

        suspend fun chapter(
            mangaKey: String,
            chapterKey: String,
        ) = db.mangaDao().find("mangadex", mangaKey)!!.let { m -> db.chapterDao().list(m.id).first { it.key == chapterKey } }

        suspend fun progress(
            mangaKey: String,
            chapterKey: String,
        ) = db.progressDao().get(chapter(mangaKey, chapterKey).id)
    }

    private val hub = FakeHub()
    private val clock = TestClock()
    private val phone = Device(hub, clock, "Android")
    private val web = Device(hub, clock, "Web")

    @AfterTest
    fun close() {
        phone.db.close()
        web.db.close()
    }

    private fun later() {
        clock.ms += 60_000
    }

    @Test
    fun RF124_first_sign_in_joins_the_device_with_the_account_without_duplicates() =
        runTest {
            (1..12).forEach { web.work("w$it") }
            web.account.signIn("tiago@example.com") shouldBe SignInResult.Done(0)
            // 10 on the phone, 8 of them also in the account.
            (5..12).forEach { phone.work("w$it") }
            (1..2).forEach { phone.work("p$it") }
            later()
            phone.account.signIn("tiago@example.com") shouldBe SignInResult.Done(4)
            phone.libraryKeys().size shouldBe 14
            later()
            web.account.syncNow()
            web.libraryKeys() shouldContainExactlyInAnyOrder phone.libraryKeys()
            phone.account.account.first() shouldBe AccountState.SignedIn("tiago@example.com")
        }

    @Test
    fun RF121_progress_on_the_phone_opens_on_the_web_at_the_same_page() =
        runTest {
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            val (_, chs) = phone.work("frieren")
            later()
            phone.chapters.saveProgress(chs[11], 8, 22)
            phone.history.record(chs[11], clock.ms)
            phone.account.syncNow()
            web.account.syncNow()
            web.progress("frieren", "c12")!!.page shouldBe 8
            web.db
                .historyDao()
                .get(web.chapter("frieren", "c12").id)!!
                .readAt shouldBe clock.ms
            web.libraryKeys() shouldBe listOf("frieren")
        }

    @Test
    fun RF123_read_on_the_web_and_opened_offline_on_the_phone_stays_read_on_both() =
        runTest {
            val (_, phoneChs) = phone.work("x")
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            // The web opens the work: its chapters come from the source.
            web.work("x")
            val webChapter = web.chapter("x", "c3").id
            later()
            web.chapters.setRead(listOf(webChapter), true)
            web.account.syncNow()
            later()
            hub.online = false
            phone.chapters.saveProgress(phoneChs[2], 3, 20)
            phone.account.syncNow()
            phone.account.status.value.error shouldBe SyncError.OFFLINE
            hub.online = true
            later()
            phone.account.syncNow()
            phone.progress("x", "c3")!!.read shouldBe true
            later()
            phone.account.syncNow()
            web.account.syncNow()
            web.progress("x", "c3")!!.read shouldBe true
        }

    @Test
    fun RF123_marked_unread_on_one_device_becomes_unread_on_the_other() =
        runTest {
            val (_, chs) = phone.work("x")
            phone.chapters.setRead(listOf(chs[0]), true)
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            web.progress("x", "c1")!!.read shouldBe true
            later()
            phone.chapters.setRead(listOf(chs[0]), false)
            phone.account.syncNow()
            web.account.syncNow()
            web.progress("x", "c1")!!.read shouldBe false
        }

    @Test
    fun RF123_taken_out_of_the_library_on_one_device_leaves_the_other() =
        runTest {
            val (id, _) = phone.work("x")
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            web.libraryKeys() shouldBe listOf("x")
            later()
            phone.mangas.removeFromLibrary(id)
            phone.account.syncNow()
            web.account.syncNow()
            web.libraryKeys() shouldBe emptyList()
        }

    @Test
    fun RF121_categories_and_their_works_follow_the_account() =
        runTest {
            val (id, _) = phone.work("x")
            val lendo = phone.categories.create("Lendo")
            phone.categories.setCategories(id, setOf(lendo.id))
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            val category =
                web.db
                    .categoryDao()
                    .list()
                    .single()
            category.name shouldBe "Lendo"
            web.db
                .categoryDao()
                .allAssignments()
                .single()
                .categoryId shouldBe category.id
            later()
            phone.categories.delete(lendo.id)
            phone.account.syncNow()
            web.account.syncNow()
            web.db.categoryDao().list() shouldBe emptyList()
        }

    @Test
    fun RF121_downloads_and_device_settings_stay_on_the_device() =
        runTest {
            phone.prefs.setWifiOnlyDownloads(false)
            phone.prefs.setContentLanguages(setOf("en", "pt-br"))
            phone.work("x")
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            web.prefs.wifiOnlyDownloads.value shouldBe true
            web.prefs.contentLanguages.value shouldBe setOf("en", "pt-br")
            hub.bodies.joinToString() shouldNotContain "downloads_wifi_only"
        }

    @Test
    fun RF122_changes_made_offline_wait_and_go_when_the_network_returns() =
        runTest {
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            hub.online = false
            phone.work("a")
            phone.work("b")
            phone.account.syncNow()
            hub.online = true
            later()
            phone.account.syncNow()
            web.account.syncNow()
            web.libraryKeys() shouldContainExactlyInAnyOrder listOf("a", "b")
        }

    @Test
    fun RF122_a_second_cycle_without_changes_sends_nothing() =
        runTest {
            phone.work("a")
            phone.account.signIn("tiago@example.com")
            val sent = hub.rows.size
            phone.account.syncNow()
            hub.bodies.last() shouldBe """{"cursor":${hub.seq},"changes":[]}"""
            hub.rows.size shouldBe sent
        }

    @Test
    fun RF120_an_account_outside_the_list_is_refused() =
        runTest {
            phone.account.signIn("someone@example.com") shouldBe SignInResult.NotAllowed
            phone.account.account.value shouldBe AccountState.SignedOut
        }

    @Test
    fun RF119_signing_out_keeps_the_library_and_stops_syncing() =
        runTest {
            phone.work("a")
            phone.account.signIn("tiago@example.com")
            phone.account.signOut()
            phone.account.account.value shouldBe AccountState.SignedOut
            phone.libraryKeys() shouldBe listOf("a")
            val calls = hub.bodies.size
            phone.account.syncNow()
            hub.bodies.size shouldBe calls
        }

    @Test
    fun RF125_a_lost_session_shows_as_a_session_error_and_keeps_the_last_success() =
        runTest {
            phone.account.signIn("tiago@example.com")
            val success = phone.account.status.value.lastSuccessAt
            hub.sessions.clear()
            later()
            phone.account.syncNow()
            phone.account.status.value.error shouldBe SyncError.SESSION
            phone.account.status.value.lastSuccessAt shouldBe success
        }

    @Test
    fun RF121_the_changes_of_the_sync_itself_do_not_bounce_back() =
        runTest {
            (1..3).forEach { phone.work("p$it") }
            phone.account.signIn("tiago@example.com")
            web.account.signIn("tiago@example.com")
            val seq = hub.seq
            later()
            web.account.syncNow()
            phone.account.syncNow()
            hub.seq shouldBe seq
            hub.bodies.takeLast(2).forEach { it.contains("\"changes\":[]") shouldBe true }
            hub.rows.values.all {
                it.change["deleted"]!!
                    .jsonPrimitive.boolean
                    .not()
            } shouldBe true
        }
}
