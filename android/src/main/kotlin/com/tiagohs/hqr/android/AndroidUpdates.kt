package com.tiagohs.hqr.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tiagohs.hqr.data.update.ReleaseChecker
import com.tiagohs.hqr.domain.repository.UpdateInfo
import com.tiagohs.hqr.domain.repository.Updates
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import java.io.File
import java.security.MessageDigest

/**
 * The updater of ADR-015 (RF-82): the APK goes to the app's cache, its SHA-256 must match the one published with the
 * release, then the system installer opens. Without permission to install apps, the system screen for it opens first.
 */
class AndroidUpdates(
    private val context: Context,
    private val checker: ReleaseChecker,
    private val http: HttpClient,
) : Updates {
    override suspend fun check(force: Boolean): Result<UpdateInfo?> = runCatchingNotCancel { checker.check(force) }

    override suspend fun install(
        info: UpdateInfo,
        progress: (Float) -> Unit,
    ): Result<Unit> =
        runCatchingNotCancel {
            val expected = info.sha256 ?: error("A versão não publicou o SHA-256")
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                error("Permita instalar apps do HQR e toque em Atualizar de novo")
            }
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(dir, "hqr-${info.version}.apk")
            val digest = MessageDigest.getInstance("SHA-256")
            http.prepareGet(info.apkUrl).execute { response ->
                val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                val channel = response.bodyAsChannel()
                apk.outputStream().use { out ->
                    val buffer = ByteArray(CHUNK)
                    var done = 0L
                    while (true) {
                        val n = channel.readAvailable(buffer, 0, buffer.size)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        if (total > 0) progress(done.toFloat() / total)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected) {
                apk.delete()
                error("O arquivo baixado não confere com o SHA-256 publicado")
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

    private inline fun <T> runCatchingNotCancel(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(e)
        }

    private companion object {
        const val CHUNK = 64 * 1024
    }
}
