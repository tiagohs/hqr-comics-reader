package com.tiagohs.hqr.download

import com.jakewharton.rxrelay2.BehaviorRelay

class DownloadManager(
        val downloader: Downloader,
        val cache: DownloadCache,
        val provider: DownloadProvider
) {

    val runningRelay: BehaviorRelay<Boolean>
        get() = downloader.runningRelay

    fun clearQueue(isNotification: Boolean) {
        downloader.clearQueue(isNotification)
    }

    fun startDownloads(): Boolean {
        return downloader.start()
    }

    fun stopDownloads(reason: String? = null) {
        downloader.stop(reason)
    }
}