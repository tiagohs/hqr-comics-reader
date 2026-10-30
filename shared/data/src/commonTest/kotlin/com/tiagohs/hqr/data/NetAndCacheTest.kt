package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.net.DiskImageCache
import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.bytes
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.ThemeMode
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NetAndCacheTest {
    private fun client(
        status: HttpStatusCode,
        seen: MutableList<String> = mutableListOf(),
    ) = HttpClient(
        MockEngine { request ->
            seen += request.headers["Referer"].orEmpty()
            respond(ByteReadChannel(byteArrayOf(1, 2, 3)), status, headersOf())
        },
    )

    @Test
    fun RF05_statusesBecomeSourceFailures() =
        runTest {
            val cases =
                mapOf(
                    HttpStatusCode.Forbidden to SourceException.Kind.AUTH,
                    HttpStatusCode.NotFound to SourceException.Kind.NOT_FOUND,
                    HttpStatusCode.TooManyRequests to SourceException.Kind.SERVER,
                    HttpStatusCode.BadGateway to SourceException.Kind.SERVER,
                    HttpStatusCode.MultipleChoices to SourceException.Kind.SERVER,
                )
            cases.forEach { (status, kind) ->
                shouldThrow<SourceException> { client(status).get("https://x/").ensureOk() }.kind shouldBe kind
            }
        }

    @Test
    fun RF07_bytesSendTheSourceHeaders() =
        runTest {
            val seen = mutableListOf<String>()
            client(HttpStatusCode.OK, seen).bytes("https://x/1.jpg", mapOf("Referer" to "https://site/")).toList() shouldBe
                listOf<Byte>(1, 2, 3)
            seen shouldBe listOf("https://site/")
        }

    @Test
    fun RF05_transportErrorIsANetworkFailure() =
        runTest {
            val broken = HttpClient(MockEngine { error("offline") })
            shouldThrow<SourceException> { broken.bytes("https://x/") }.kind shouldBe SourceException.Kind.NETWORK
        }

    @Test
    fun RF04_limiterWaitsInsteadOfFailing() =
        runTest {
            var now = 0L
            val waits = mutableListOf<Duration>()
            val clock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(now)
                }
            val limiter =
                RateLimiter(2, 1.seconds, clock) {
                    waits += it
                    now += it.inWholeMilliseconds
                }
            repeat(3) { limiter.acquire() }
            waits shouldBe listOf(1.seconds)
        }

    @Test
    fun RNF23_memoryCacheDropsTheOldest() =
        runTest {
            val cache = MemoryImageCache(maxBytes = 10)
            cache.put("a", ByteArray(4))
            cache.put("b", ByteArray(4))
            cache.get("a")
            cache.put("c", ByteArray(4))
            cache.get("b") shouldBe null
            cache.get("a")?.size shouldBe 4
            cache.size() shouldBe 8
            cache.clear()
            cache.size() shouldBe 0
        }

    @Test
    fun RNF23_diskCacheStaysUnderItsLimit() =
        runTest {
            val fs = FakeFileSystem()
            val cache = DiskImageCache(fs, "/cache".toPath(), maxBytes = 10)
            cache.get("a") shouldBe null
            cache.put("a", ByteArray(6))
            cache.get("a")?.size shouldBe 6
            cache.put("b", ByteArray(6))
            (cache.size() <= 10) shouldBe true
            cache.clear()
            cache.size() shouldBe 0
        }

    @Test
    fun RF80_clearingTheCacheKeepsTheDownloads() =
        runTest {
            val fs = FakeFileSystem()
            val cache = DiskImageCache(fs, "/cache/images".toPath())
            val downloads =
                com.tiagohs.hqr.data.download
                    .DownloadFiles(fs, "/files/downloads".toPath())
            cache.put("https://img/1", ByteArray(300))
            downloads.write(7, 0, ByteArray(50))
            cache.size() shouldBe 300
            cache.clear()
            cache.size() shouldBe 0
            downloads.has(7, 0) shouldBe true
            downloads.usedBytes() shouldBe 50
        }

    @Test
    fun RF76_preferencesPersist() {
        val settings = MapSettings()
        AppPreferences(settings).apply {
            theme.value shouldBe ThemeMode.SYSTEM
            contentLanguages.value shouldBe setOf("pt-br")
            setTheme(ThemeMode.DARK)
            setPureBlack(true)
            setCoverColor(false)
            setLocalFolder("content://tree/1")
            setOnboarded()
            setContentLanguages(setOf("en", "pt-br"))
            setWifiOnlyDownloads(false)
            putString("k", "v")
            putBoolean("b", true)
        }
        AppPreferences(settings).apply {
            theme.value shouldBe ThemeMode.DARK
            pureBlack.value shouldBe true
            coverColor.value shouldBe false
            localFolder.value shouldBe "content://tree/1"
            onboarded.value shouldBe true
            contentLanguages.value shouldBe setOf("en", "pt-br")
            wifiOnlyDownloads.value shouldBe false
            getString("k") shouldBe "v"
            getBoolean("b", false) shouldBe true
            setLocalFolder(null)
            putString("k", null)
            getString("k") shouldBe null
        }
        AppPreferences(settings).localFolder.value shouldBe null
    }

    @Test
    fun RF06_mangaDexLanguagesFollowTheContentOnesUntilChosenAndSaverPersists() {
        val settings = MapSettings()
        AppPreferences(settings).apply {
            setContentLanguages(setOf("en", "pt-br"))
            mangaDexLanguages() shouldBe setOf("en", "pt-br")
            mangaDexLanguagesOverride.value shouldBe null
            setMangaDexLanguages(setOf("pt-br"))
            mangaDexDataSaver.value shouldBe false
            setMangaDexDataSaver(true)
        }
        AppPreferences(settings).apply {
            mangaDexLanguages() shouldBe setOf("pt-br")
            mangaDexDataSaver.value shouldBe true
            // Back to following the content languages.
            setMangaDexLanguages(null)
            mangaDexLanguages() shouldBe setOf("en", "pt-br")
        }
        AppPreferences(settings).mangaDexLanguagesOverride.value shouldBe null
    }

    @Test
    fun RF08_suggestiveOnlyWhenTurnedOn() {
        val prefs = AppPreferences(MapSettings())
        prefs.mangaDexRatings() shouldBe setOf("safe")
        prefs.setMangaDexSuggestive(true)
        prefs.mangaDexRatings() shouldBe setOf("safe", "suggestive")
    }
}
