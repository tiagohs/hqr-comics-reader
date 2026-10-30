package com.tiagohs.hqr.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tiagohs.hqr.data.update.ApkDownloader
import com.tiagohs.hqr.data.update.ReleaseChecker
import com.tiagohs.hqr.domain.repository.UpdateFailure
import com.tiagohs.hqr.domain.repository.UpdateInfo
import com.tiagohs.hqr.domain.repository.UpdateProblem
import com.tiagohs.hqr.domain.repository.Updates
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * The updater of ADR-015 (RF-82): the APK goes to the app's cache, its SHA-256 must match the one published with the
 * release, then the system installer opens. Without permission to install apps, the system screen for it opens first.
 */
class AndroidUpdates(
    private val context: Context,
    private val checker: ReleaseChecker,
    http: HttpClient,
) : Updates {
    private val downloader = ApkDownloader(http, FileSystem.SYSTEM)

    override suspend fun check(force: Boolean): Result<UpdateInfo?> = runCatchingNotCancel { checker.check(force) }

    override suspend fun install(
        info: UpdateInfo,
        progress: (Float) -> Unit,
    ): Result<Unit> =
        runCatchingNotCancel {
            if (info.sha256 == null) throw UpdateFailure(UpdateProblem.NO_CHECKSUM)
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                throw UpdateFailure(UpdateProblem.NEEDS_PERMISSION)
            }
            val apk = downloader.download(info, File(context.cacheDir, "updates").toOkioPath(), progress).toFile()
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
}
