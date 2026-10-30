package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.update.ApkDownloader
import com.tiagohs.hqr.domain.repository.UpdateFailure
import com.tiagohs.hqr.domain.repository.UpdateInfo
import com.tiagohs.hqr.domain.repository.UpdateProblem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test

/** The APK of an update: downloaded, checked against the published SHA-256, never kept when wrong (RF-82, ADR-015). */
class ApkDownloaderTest {
    private val apk = ByteArray(200_000) { (it % 251).toByte() }
    private val sha = apk.toByteString().sha256().hex()
    private var status = HttpStatusCode.OK
    private val http =
        HttpClient(MockEngine { respond(apk, status, headersOf(HttpHeaders.ContentLength, apk.size.toString())) }) {
            install(HttpTimeout) { requestTimeoutMillis = 1_000 }
        }
    private val fs = FakeFileSystem()
    private val dir = "/cache/updates".toPath()
    private val downloader = ApkDownloader(http, fs)

    private fun info(sha256: String? = sha) = UpdateInfo("1.1.0", "", "https://gh/hqr-1.1.0.apk", sha256)

    @Test
    fun RF82_rightChecksumKeepsTheApkAndReportsProgress() =
        runTest {
            fs.createDirectories(dir)
            fs.write(dir / "hqr-1.0.9.apk") { writeUtf8("old") }
            val steps = mutableListOf<Float>()
            val path = downloader.download(info(sha.uppercase()), dir) { steps += it }
            fs.read(path) { readByteArray() }.contentEquals(apk) shouldBe true
            steps.last() shouldBe 1f
            // Only the new APK is kept.
            fs.list(dir) shouldBe listOf(path)
        }

    @Test
    fun RF82_checksumMismatchIsRefusedAndDeleted() =
        runTest {
            shouldThrow<UpdateFailure> { downloader.download(info("00ff"), dir) {} }.problem shouldBe UpdateProblem.CHECKSUM_MISMATCH
            fs.list(dir) shouldBe emptyList()
        }

    @Test
    fun RF82_noChecksumIsRefusedBeforeDownloading() =
        runTest {
            shouldThrow<UpdateFailure> { downloader.download(info(null), dir) {} }.problem shouldBe UpdateProblem.NO_CHECKSUM
            fs.exists(dir) shouldBe false
        }

    @Test
    fun RF82_serverErrorIsANetworkProblemNotAWrongFile() =
        runTest {
            status = HttpStatusCode.NotFound
            shouldThrow<UpdateFailure> { downloader.download(info(), dir) {} }.problem shouldBe UpdateProblem.NETWORK
            fs.list(dir) shouldBe emptyList()
        }

    @Test
    fun RF82_onlyHttps() =
        runTest {
            val plain = info().copy(apkUrl = "http://gh/hqr.apk")
            shouldThrow<UpdateFailure> { downloader.download(plain, dir) {} }.problem shouldBe UpdateProblem.NETWORK
        }
}
