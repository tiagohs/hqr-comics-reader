package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.downloads.DownloadItem
import com.tiagohs.hqr.ui.contracts.DownloadContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class DownloadPresenter(
        private val preferenceHelper: PreferenceHelper,
        private val comicRepository: IComicsRepository,
        private val localeUtils: LocaleUtils,
        private val downloadManager: DownloadManager,
        private val provider: DownloadProvider
): BasePresenter<DownloadContract.IDownloadView>(), DownloadContract.IDownloadPresenter {

    var listPaginator: ListPaginator<DownloadItem> = ListPaginator()

    override fun onGetDownloads() {

        mSubscribers.add(comicRepository.getDownloadedComics()
                .map { onFilterDownloads(it) }
                .subscribe({ downloads -> mView?.onBindDownloads(downloads) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))

    }

    private fun onFilterDownloads(comics: List<ComicViewModel>): List<DownloadItem> {
        val finalDownloadList = ArrayList<DownloadItem>()

        comics.forEach {
            val chaptersDownloaded = provider.findComicDirectory(it, it.source!!)?.listFiles()

            if (chaptersDownloaded == null || chaptersDownloaded.isEmpty()) {
                comicRepository.setAsNotDownloaded(it, it.source!!.id).subscribe()
            } else {
                finalDownloadList.add(it.toModel())
            }
        }

        return finalDownloadList.toList()
    }

    override fun onGetMore() {
        mSubscribers.add(listPaginator.onGetNextPage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindMoreDownloads(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    override fun hasMore(): Boolean {
        return listPaginator.hasMorePages
    }

    override fun getOriginalList(): List<DownloadItem> {
        return listPaginator.originalList
    }

    override fun deleteComic(downloadItem: DownloadItem) {
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
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    private fun ComicViewModel.toModel(): DownloadItem {
        val chaptersDownloaded = ArrayList<ChapterViewModel>()

        this.chapters?.forEach {
            val chapter = provider.findChapterDirectory(it, this@toModel, this.source!!)

            if (chapter != null) {
                chaptersDownloaded.add(it)
            }
        }

        return DownloadItem(this, chaptersDownloaded, localeUtils)
    }

}