package com.tiagohs.hqr.ui.presenter

import android.content.Context
import android.util.Log
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.downloads.DownloadItem
import com.tiagohs.hqr.ui.contracts.DownloadContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class DownloadPresenter(
        private val preferenceHelper: PreferenceHelper,
        private val comicRepository: IComicsRepository,
        private val historyRepository: IHistoryRepository,
        private val localeUtils: LocaleUtils,
        private val downloadManager: DownloadManager,
        private val provider: DownloadProvider
): BasePresenter<DownloadContract.IDownloadView>(), DownloadContract.IDownloadPresenter {

    var listPaginator: ListPaginator<DownloadItem> = ListPaginator()

    override fun onGetDownloads(context: Context?) {

        mSubscribers.add(comicRepository.getDownloadedComics()
                .map { onFilterDownloads(it, context) }
                .subscribe({ histories -> mView?.onBindDownloads(histories) },
                        { error -> Log.e("DOWNLOADS", "onGetDownloads Falhou ", error) }))

    }

    private fun onFilterDownloads(comics: List<ComicViewModel>, context: Context?): List<DownloadItem> {
        val finalDownloadList = ArrayList<DownloadItem>()

        comics.forEach {
            val chaptersDownloaded = provider.findComicDirectory(it, it.source!!)?.listFiles()

            if (chaptersDownloaded == null || chaptersDownloaded.isEmpty()) {
                comicRepository.setAsNotDownloaded(it, it.source!!.id).subscribe()
            } else {
                finalDownloadList.add(it.toModel(context))
            }
        }

        return finalDownloadList.toList()
    }

    override fun onGetMore() {
        mSubscribers.add(listPaginator.onGetNextPage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindMoreDownloads(it) },
                        { error -> Log.e("DOWNLOADS", "onGetMore Falhou ", error) }))
    }

    override fun hasMore(): Boolean {
        return listPaginator.hasMorePages
    }

    override fun getOriginalList(): List<DownloadItem> {
        return listPaginator.originalList
    }

    override fun deleteChapters(downloadItem: DownloadItem) {
        downloadManager.deleteComic(downloadItem.comic, downloadItem.comic.source!!)
    }

    override fun addOrRemoveFromFavorite(downloadItem: DownloadItem) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        mSubscribers.add(comicRepository.addOrRemoveFromFavorite(downloadItem.comic, sourceId)
                .subscribeOn(Schedulers.io())
                .map {
                    downloadItem.comic.favorite = it.favorite
                    downloadItem
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindItem(it) },
                        { error -> Log.e("DOWNLOADS", "addOrRemoveFromFavorite Falhou ", error) }))
    }

    private fun ComicViewModel.toModel(context: Context?): DownloadItem {
        return DownloadItem(this, localeUtils, context!!)
    }

}