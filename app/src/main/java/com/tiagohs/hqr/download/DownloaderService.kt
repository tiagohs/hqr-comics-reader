package com.tiagohs.hqr.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import io.reactivex.disposables.CompositeDisposable

class DownloaderService(
    downloadManager: DownloadManager,
    subscription: CompositeDisposable

): Service() {

    companion object {

        fun startDownloadService(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun watchDownloadState() {

    }

    fun watchNetworkState() {

    }


}