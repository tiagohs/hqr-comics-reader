package com.tiagohs.hqr.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.repository.DownloadState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runs the download queue in the background (ADR-009, RF-63): WorkManager keeps it across app restarts and reboots,
 * on an unmetered network when "só no Wi-Fi" is on (RF-64), with a progress notification (RF-94: the queue runs even
 * if the notification permission was denied).
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val manager: DownloadManager by inject()
    private val prefs: AppPreferences by inject()

    override suspend fun doWork(): Result =
        coroutineScope {
            runCatching { setForeground(foreground(0, 0, "")) }
            val progress =
                launch {
                    manager.observe().collect { items ->
                        val running = items.firstOrNull { it.state == DownloadState.RUNNING } ?: return@collect
                        runCatching { setForeground(foreground(running.pagesDone, running.pagesTotal, running.mangaTitle)) }
                    }
                }
            val waiting = manager.run { allowed() }
            progress.cancel()
            // RF-68: the queue stopped for lack of space; the user hears it even with the app closed.
            if (manager.observe().first().any { it.lowSpace }) lowSpaceNotice()
            // Chapters left waiting for the network: WorkManager tries again later, under the run's constraint (RF-63).
            if (waiting) Result.retry() else Result.success()
        }

    private fun allowed(): Boolean {
        val connectivity = applicationContext.getSystemService(ConnectivityManager::class.java)
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return !prefs.wifiOnlyDownloads.value || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun foreground(
        done: Int,
        total: Int,
        title: String,
    ): ForegroundInfo {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, applicationContext.getString(R.string.downloads_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(applicationContext.getString(R.string.downloads_running))
                .setContentText(title)
                .setProgress(total, done, total == 0)
                .setOngoing(true)
                .setSilent(true)
                .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun lowSpaceNotice() {
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(applicationContext.getString(R.string.downloads_low_space_title))
                .setContentText(applicationContext.getString(R.string.downloads_low_space_text))
                .setAutoCancel(true)
                .build()
        // Without the notification permission (RF-94), the reason still shows on the downloads screen.
        runCatching { NotificationManagerCompat.from(applicationContext).notify(LOW_SPACE_ID, notification) }
    }

    companion object {
        private const val LOW_SPACE_ID = 43
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_ID = 42
        const val NAME = "hqr-downloads"
    }
}

/** One unique worker for the queue; the network rule follows "só no Wi-Fi" (RF-64). */
class WorkManagerDownloadScheduler(
    private val context: Context,
    private val prefs: AppPreferences,
) : DownloadScheduler {
    override fun schedule(restart: Boolean) {
        val network = if (prefs.wifiOnlyDownloads.value) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
                .build()
        // A queue change while it runs is picked up when the current run ends; a new network rule (RF-64) replaces the
        // run waiting under the old one.
        val policy = if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(DownloadWorker.NAME, policy, request)
    }
}
