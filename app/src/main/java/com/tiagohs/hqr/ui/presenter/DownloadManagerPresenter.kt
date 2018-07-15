package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloaderService
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.ui.adapters.downloads_queue.DownloadQueueItem
import com.tiagohs.hqr.ui.contracts.DownloadManagerContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DownloadManagerPresenter(
        val downloadManager: DownloadManager
): BasePresenter<DownloadManagerContract.IDownloadManagerView>(), DownloadManagerContract.IDownloadManagerPresenter {

    private val progressSubscribes: HashMap<Download, Disposable> = HashMap()
    private val downloadQueue = downloadManager.queue


    override fun onUnbindView() {
        super.onUnbindView()

        progressSubscribes.forEach { download, disposable -> disposable.dispose() }
        progressSubscribes.clear()
    }

    override fun onCreate() {

        mSubscribers.add(DownloaderService.runningRelay
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe({ mView?.onQueueStatusChange(it) },
                         { error ->
                             Timber.e(error)
                             mView?.onError(error)
                         }
                 ))

        mSubscribers.add(downloadQueue.getUpdatedStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { ArrayList(it.map { it.toModel() }) }
                .subscribe({ mView?.onNextDownloads(it) },
                    { error ->
                        Timber.e(error)
                        mView?.onError(error)
                    }
                ))

        mSubscribers.add(downloadQueue.getStatus()
                .subscribeOn(Schedulers.io())
                .startWith(downloadQueue.getActiviteDownloads())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onStatusChange(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }
                ))

        mSubscribers.add(downloadQueue.getProgress()
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer()
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onProgressChange(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }
                ))
    }

    private fun onStatusChange(download: Download) {

        when(download.status) {
            Download.DOWNLOADING -> {
                observeProgress(download)

                mView?.onProgressChange(download)
            }
            Download.DOWNLOADED -> {
                unsubscribeProgress(download)

                mView?.onUpdateProgress(download)
                mView?.onProgressChange(download)
            }
        }
    }

    private fun unsubscribeProgress(download: Download) {
        progressSubscribes.remove(download)
    }

    private fun observeProgress(download: Download) {
        val subscription = Observable.interval(50, TimeUnit.MILLISECONDS)
                .flatMap {
                    Observable.fromIterable(download.chapter.pages)
                            .map(Page::progress)
                            .reduce { x, y -> x + y }
                            .toObservable()
                }
                .toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ progress ->
                    if (download.progressTotal != progress) {
                        download.progressTotal = progress
                        mView?.onUpdateProgress(download)
                    }
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                })

        progressSubscribes.remove(download)?.dispose()
        progressSubscribes.put(download, subscription)
    }

    override fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    override fun clearQueue() {
        downloadManager.clearQueue(false)
    }

    override fun removeFromQueue(download: Download) {
        downloadQueue.remove(download)
    }

    override fun isQueueEmpty(): Boolean {
        return downloadQueue.isEmpty()
    }

    private fun Download.toModel(): DownloadQueueItem {
        return DownloadQueueItem(this)
    }

}