package com.tiagohs.hqr.data

import com.russhwolf.settings.MapSettings
import com.tiagohs.hqr.data.update.ReleaseChecker
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ReleaseCheckerTest {
    private var now = Instant.fromEpochMilliseconds(1_790_000_000_000)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private var release =
        """{"tag_name":"v1.1.0","body":"- Novidade","assets":[
        {"name":"hqr-1.1.0.apk","browser_download_url":"https://gh/hqr-1.1.0.apk"},
        {"name":"hqr-1.1.0.apk.sha256","browser_download_url":"https://gh/hqr-1.1.0.apk.sha256"}]}"""
    private var calls = 0
    private val http =
        HttpClient(
            MockEngine { request ->
                calls++
                if (request.url.toString().endsWith(".sha256")) {
                    respond("ABCDEF  hqr-1.1.0.apk\n", HttpStatusCode.OK)
                } else {
                    respond(release, HttpStatusCode.OK)
                }
            },
        )
    private val checker = ReleaseChecker(http, MapSettings(), "1.0.0", clock)

    @Test
    fun RF82_newerReleaseWithNotesApkAndChecksum() =
        runTest {
            val info = checker.check(force = false)!!
            info.version shouldBe "1.1.0"
            info.notes shouldBe "- Novidade"
            info.apkUrl shouldBe "https://gh/hqr-1.1.0.apk"
            info.sha256 shouldBe "abcdef"
        }

    @Test
    fun RF82_onceADayUnlessAsked() =
        runTest {
            checker.check(force = false)
            val after = calls
            checker.check(force = false) shouldBe null
            calls shouldBe after
            checker.check(force = true)!!.version shouldBe "1.1.0"
            now += 25.hours
            checker.check(force = false)!!.version shouldBe "1.1.0"
        }

    @Test
    fun RF84_sameOrOlderOrPrereleaseIsNoUpdate() =
        runTest {
            release = """{"tag_name":"v1.0.0","assets":[]}"""
            checker.check(force = true) shouldBe null
            release = """{"tag_name":"v2.0.0","prerelease":true,"assets":[{"name":"a.apk","browser_download_url":"u"}]}"""
            checker.check(force = true) shouldBe null
            ReleaseChecker.newer("v1.10.0", "1.9.9") shouldBe true
            ReleaseChecker.newer("0.0.5", "1.0.0") shouldBe false
        }
}
