package com.tiagohs.hqr.models

import com.jakewharton.rxrelay2.PublishRelay
import com.tiagohs.hqr.download.DownloadStore
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject

class DownloadQueueList(
        val store: DownloadStore,
        val queue: ArrayList<Download> = ArrayList<Download>()
): List<Download> by queue {

    private val statusSubject = PublishProcessor.create<Download>()
    private val updatedRelay = PublishRelay.create<Unit>()

    fun addAll(downloads: List<Download>) {
        downloads.forEach { download: Download ->
            download.setStatusSubject(statusSubject)
            download.status = Download.QUEUE
        }

        queue.addAll(downloads)
        store.addAll(downloads)

        updatedRelay.accept(Unit)
    }

    fun remove(download: Download) {
        val removed = queue.remove(download)
        download.setStatusSubject(null)
        store.remove(download)

        if (removed)
            updatedRelay.accept(Unit)
    }

    fun remove(chapter: ChapterViewModel) {
        find { download -> download.chapter.chapterPath === chapter.chapterPath }?.let { download -> remove(download) }
    }

    fun clear() {
        queue.forEach({ download: Download ->
            download.setStatusSubject(null)
        })

        queue.clear()
        store.clear()
        updatedRelay.accept(Unit)
    }

    fun getActiviteDownloads(): Observable<Download> {
        return Observable.fromIterable(this).filter({ download: Download -> download.status === Download.DOWNLOADING })
    }

    fun getStatus(): Observable<Download> = statusSubject.onBackpressureBuffer().toObservable()

    fun getUpdatedStatus(): Observable<List<Download>> =
            updatedRelay
                    .toFlowable(BackpressureStrategy.BUFFER)
                    .onBackpressureBuffer()
                    .startWith(Unit)
                    .toObservable()
                    .map{ this }

    fun getProgress(): Observable<Download> {
        return statusSubject.onBackpressureBuffer()
                .toObservable()
                .startWith(getActiviteDownloads())
                .flatMap { download: Download ->

                    if (download.status == Download.DOWNLOADING) {
                        val pageStatus = PublishSubject.create<String>()
                        setPagesSubject(download.chapter.pages, pageStatus)

                        return@flatMap pageStatus
                                .toFlowable(BackpressureStrategy.BUFFER)
                                .onBackpressureBuffer()
                                .filter { it == Page.READY }
                                .map { download }
                                .toObservable()
                    } else if (download.status == Download.DOWNLOADED || download.status == Download.ERROR) {
                        setPagesSubject(download.chapter.pages, null)
                    }

                    Observable.just(download)
                }

    }

    private fun setPagesSubject(pages: List<Page>?, subject: PublishSubject<String>?) {
        if (pages != null) {
            for (page in pages) {
                page.setStatusSubject(subject!!)
            }
        }
    }
}