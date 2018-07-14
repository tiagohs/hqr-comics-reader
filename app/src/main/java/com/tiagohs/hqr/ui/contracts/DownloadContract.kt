package com.tiagohs.hqr.ui.contracts

import android.content.Context
import com.tiagohs.hqr.ui.adapters.downloads.DownloadItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class DownloadContract {

    interface IDownloadView: IView {
        fun onBindItem(downloadItem: DownloadItem)
        fun onBindDownloads(downloads: List<DownloadItem>)
        fun onBindMoreDownloads(downloads: List<DownloadItem>)
    }

    interface IDownloadPresenter: IPresenter<IDownloadView> {

        fun onGetDownloads(context: Context?)
        fun onGetMore()

        fun hasMore(): Boolean
        fun getOriginalList(): List<DownloadItem>

        fun deleteChapters(downloadItem: DownloadItem)
        fun addOrRemoveFromFavorite(downloadItem: DownloadItem)
    }
}