package com.tiagohs.hqr.download

import com.jakewharton.rxrelay2.BehaviorRelay
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.models.DownloadQueueList
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.reactivex.Observable
import javax.inject.Singleton

@Singleton
class DownloadManager(
        val downloader: Downloader,
        val cache: DownloadCache,
        val provider: DownloadProvider,
        val comicRepository: IComicsRepository
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

    fun downloadChapters(comic: ComicViewModel, chapter: List<ChapterViewModel>, autoStart: Boolean = true) {
        downloader.queuerChapters(comic, chapter, autoStart)
    }

    fun buildListOfPages(source: SourceDB, comic: ComicViewModel, chapter: ChapterViewModel): Observable<List<Page>> {
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

    fun isChapterDownloaded(chapter: ChapterViewModel, comic: ComicViewModel, skipCache: Boolean = false): Boolean {
        return cache.isChapterDownloaded(comic, chapter, skipCache)
    }

    fun getDownloadCount(comic: ComicViewModel): Int {
        return cache.getDownloadCount(comic)
    }

    fun deleteChapter(chapter: ChapterViewModel, comic: ComicViewModel, source: SourceDB) {
        provider.findChapterDirectory(chapter, comic, source)?.delete()
        cache.removeChapter(chapter, comic)

        val hasDownloads = provider.findComicDirectory(comic, source)?.listFiles()?.isNotEmpty() ?: false
        if (hasDownloads) {
            comicRepository.setAsNotDownloaded(comic, source.id)
        }
    }

    fun deleteComic(comic: ComicViewModel, source: SourceDB) {
        provider.findComicDirectory(comic, source)?.delete()
        cache.removeManga(comic)
        comicRepository.setAsNotDownloaded(comic, source.id)
    }

}