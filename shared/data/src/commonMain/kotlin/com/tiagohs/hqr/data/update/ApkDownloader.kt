package com.tiagohs.hqr.data.update

import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.domain.repository.UpdateFailure
import com.tiagohs.hqr.domain.repository.UpdateInfo
import com.tiagohs.hqr.domain.repository.UpdateProblem
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.use

/**
 * Downloads the APK of a release into [dir] and checks it against the published SHA-256 (RF-82, ADR-015). Only one
 * file is kept there; any failure leaves no file behind and is an [UpdateFailure].
 */
class ApkDownloader(
    private val http: HttpClient,
    private val fs: FileSystem,
) {
    suspend fun download(
        info: UpdateInfo,
        dir: Path,
        progress: (Float) -> Unit,
    ): Path {
        val expected = info.sha256 ?: throw UpdateFailure(UpdateProblem.NO_CHECKSUM)
        if (!info.apkUrl.startsWith("https://")) throw UpdateFailure(UpdateProblem.NETWORK)
        fs.deleteRecursively(dir)
        fs.createDirectories(dir)
        val apk = dir / "hqr-${info.version}.apk"
        val actual =
            try {
                save(info.apkUrl, apk, progress)
            } catch (e: CancellationException) {
                fs.delete(apk)
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                fs.delete(apk)
                throw UpdateFailure(UpdateProblem.NETWORK, e)
            }
        if (actual != expected.lowercase()) {
            fs.delete(apk)
            throw UpdateFailure(UpdateProblem.CHECKSUM_MISMATCH)
        }
        return apk
    }

    /** Writes the body to [apk] while hashing it; returns the SHA-256 in hex. */
    private suspend fun save(
        url: String,
        apk: Path,
        progress: (Float) -> Unit,
    ): String =
        // An APK of tens of MB on a slow network takes longer than the client's request timeout.
        http.prepareGet(url) { timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS } }.execute { response ->
            response.ensureOk()
            val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
            val channel = response.bodyAsChannel()
            val hashing = HashingSink.sha256(blackholeSink())
            fs.sink(apk).buffer().use { out ->
                hashing.buffer().use { hash ->
                    val buffer = ByteArray(CHUNK)
                    var done = 0L
                    while (true) {
                        val n = channel.readAvailable(buffer, 0, buffer.size)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        hash.write(buffer, 0, n)
                        done += n
                        if (total > 0) progress(done.toFloat() / total)
                    }
                    hash.flush()
                }
            }
            hashing.hash.hex()
        }

    private companion object {
        const val CHUNK = 64 * 1024
    }
}
