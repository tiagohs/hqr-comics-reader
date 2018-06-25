package com.tiagohs.hqr.download

import com.jakewharton.rxrelay2.BehaviorRelay
import com.tiagohs.hqr.models.DownloadQueueList
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.sources.IHttpSource
import io.reactivex.Observable

class DownloadManager(
        val downloader: Downloader,
        val cache: DownloadCache,
        val provider: DownloadProvider
) {

    val queue: DownloadQueueList = downloader.queue
    val runningRelay: BehaviorRelay<Boolean> = downloader.runningRelay

    fun startDownloads(): Boolean {
        return downloader.start()
    }

    fun stopDownloads(reason: String? = null) {
        downloader.stop(reason)
    }

    fun pauseDownloads() {
        downloader.pause()
    }

    fun clearQueue(isNotification: Boolean) {
        downloader.clearQueue(isNotification)
    }

    fun downloadChapters(comic: Comic, chapter: List<Chapter>, autoStart: Boolean = true) {
        downloader.queuerChapters(comic, chapter, autoStart)
    }

    fun buildListOfPages(source: IHttpSource, comic: Comic, chapter: Chapter): Observable<List<Page>> {
        val chapterDir = provider.findChapterDirectory(chapter, comic, source)

        return Observable.fromCallable {
            val files = chapterDir?.listFiles().orEmpty()
                    .filter { "image" in it.type.orEmpty() }

            if (files.isEmpty()) {
                throw Exception("Page list is empty.")
            }

            files.sortedBy { it.name }
                    .mapIndexed { index, file ->
                        Page(index, uri = file.uri).apply { status = Page.READY }
                    }
        }
    }

    fun isChapterDownloaded(chapter: Chapter, comic: Comic, skipCache: Boolean = false): Boolean {
        return cache.isChapterDownloaded(comic, chapter, skipCache)
    }

    fun getDownloadCount(comic: Comic): Int {
        return cache.getDownloadCount(comic)
    }

    fun deleteChapter(chapter: Chapter, comic: Comic, source: IHttpSource) {
        provider.findChapterDirectory(chapter, comic, source)?.delete()
        cache.removeChapter(chapter, comic)
    }

    fun deleteComic(comic: Comic, source: IHttpSource) {
        provider.findComicDirectory(comic, source)?.delete()
        cache.removeManga(comic)
    }

}