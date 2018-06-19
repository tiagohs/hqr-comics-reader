package com.tiagohs.hqr.download

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxrelay2.BehaviorRelay
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.connectivityManager
import com.tiagohs.hqr.helpers.extensions.powerManager
import com.tiagohs.hqr.helpers.extensions.toast
import com.tiagohs.hqr.notification.Notifications
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class DownloaderService(
    val downloadManager: DownloadManager

): Service() {

    companion object {

        val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create()

        fun startDownloaderService(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }

        fun stopDownloaderService(context: Context) {
            context.stopService(Intent(context, DownloaderService::class.java))
        }

    }

    private lateinit var subscriptions: CompositeDisposable
    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadService:WakeLock")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(Notifications.ID_DOWNLOAD_CHAPTER, getPlaceholderNotification())
        runningRelay.accept(true)
        subscriptions = CompositeDisposable()

        watchNetworkState()
        watchDownloadState()
    }

    override fun onDestroy() {
        runningRelay.accept(false)
        subscriptions.dispose()
        downloadManager.stopDownloads()
        wakeLock.releaseIfNeeded()

        super.onDestroy()
    }

    fun watchNetworkState() {
        subscriptions.add(ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ connectivity -> onNetworkStateChange(connectivity) },
                    { error ->
                        toast(R.string.download_queue_error)
                        stopSelf()
                })

        )
    }

    fun watchDownloadState() {
        subscriptions.add(downloadManager.runningRelay.subscribe { running ->
            if (running)
                wakeLock.acquireIfNeeded()
            else
                wakeLock.releaseIfNeeded()
        })
    }

    fun PowerManager.WakeLock.releaseIfNeeded() {
        if (isHeld) release()
    }

    fun PowerManager.WakeLock.acquireIfNeeded() {
        if (!isHeld) acquire(5000)
    }

    private fun onNetworkStateChange(connectivity: Connectivity) {
        when (connectivity.state) {
            NetworkInfo.State.CONNECTED -> {
                if (isWifiConnected()) {
                    downloadManager.stopDownloads(getString(R.string.no_wifi))
                } else {
                    val started = downloadManager.startDownloads()
                    if (!started) stopSelf()
                }
            }
            NetworkInfo.State.DISCONNECTED -> {
                downloadManager.stopDownloads(getString(R.string.no_network))
            }
            else -> {}
        }
    }

    private fun isWifiConnected(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }


    private fun getPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, Notifications.CHANNEL_DOWNLOADER)
                .setContentTitle(getString(R.string.download_notification_downloader_title))
                .build()
    }
}