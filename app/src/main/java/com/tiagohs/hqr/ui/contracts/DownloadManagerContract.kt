package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.ui.adapters.downloads_queue.DownloadQueueItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView
class DownloadManagerContract {

    interface IDownloadManagerView: IView {
        fun onQueueStatusChange(running: Boolean)
        fun onNextDownloads(downloads: ArrayList<DownloadQueueItem>)
        fun onProgressChange(download: Download)
        fun onUpdateProgress(download: Download)
    }

    interface IDownloadManagerPresenter: IPresenter<IDownloadManagerView> {

        fun onCreate()
        fun pauseDownloads()
        fun clearQueue()
        fun isQueueEmpty(): Boolean
        fun removeFromQueue(download: Download)
    }
}