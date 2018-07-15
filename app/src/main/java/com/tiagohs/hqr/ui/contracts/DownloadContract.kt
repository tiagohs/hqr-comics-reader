package com.tiagohs.hqr.ui.contracts

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

        fun onGetDownloads()
        fun onGetMore()

        fun hasMore(): Boolean
        fun getOriginalList(): List<DownloadItem>

        fun deleteComic(downloadItem: DownloadItem)
        fun addOrRemoveFromFavorite(downloadItem: DownloadItem)
    }
}