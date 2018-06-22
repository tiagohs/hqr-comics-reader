package com.tiagohs.hqr.download

import android.content.Context
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.tiagohs.hqr.helpers.extensions.launchNow
import com.tiagohs.hqr.helpers.extensions.launchUI
import com.tiagohs.hqr.helpers.extensions.saveTo
import com.tiagohs.hqr.helpers.tools.RetryWithDelay
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.DownloadQueueList
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.notification.DownloadNotification
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.async
import okhttp3.Response

class Downloader(
        val context: Context,
        val provider: DownloadProvider,
        val sourceManager: SourceManager,
        val store: DownloadStore,
        val cache: DownloadCache,
        val downloadNotification: DownloadNotification
) {

    private val subscriptions = CompositeDisposable()
    private val relay = PublishRelay.create<List<Download>>()
    private var isRunning: Boolean = false

    val queue: DownloadQueueList = DownloadQueueList(store)
    val runningRelay: BehaviorRelay<Boolean> = BehaviorRelay.create()

    init {
        launchNow {
            val chapters = async { store.restore() }
            queue.addAll(chapters.await())
        }
    }

    fun start(): Boolean {
        if (isRunning || queue.isEmpty()) return false

        val pedingDownloads = queue.filter { download -> download.status != Download.DOWNLOADED }
        pedingDownloads.forEach { download: Download ->
            if (download.status != Download.QUEUE)
                download.status = Download.QUEUE
        }

        relay.accept(pedingDownloads)

        return !pedingDownloads.isEmpty()
    }

    fun stop(reason: String? = null) {
        onDestroyAllSubscriptions()

        queue.filter { download -> download.status == Download.DOWNLOADING }
             .forEach { download -> download.status = Download.ERROR }

        if (reason != null) {
            downloadNotification.onWarning(reason)
        } else {
            if (downloadNotification.paused) {
                downloadNotification.paused = false
                downloadNotification.onDownloadPaused()
            } else {
                downloadNotification.dimissNotification()
            }
        }
    }

    fun pause() {
        onDestroyAllSubscriptions()

        queue.filter { download -> download.status == Download.DOWNLOADING }
             .forEach { download -> download.status = Download.QUEUE }

        downloadNotification.paused = true
    }

    fun clearQueue(isNotification: Boolean = false) {
        onDestroyAllSubscriptions()

        if (isNotification) {
            queue.filter { download -> download.status == Download.DOWNLOADING }
                    .forEach { download -> download.status = Download.NOT_DOWNLOADED }
        }

        queue.clear()
        downloadNotification.dimissNotification()
    }

    fun queuerChapters(comic: Comic, chaptersToDownload: List<Chapter>, autoStart: Boolean) = launchUI {
        val source = sourceManager.get(comic.sourceId) as? HttpSourceBase ?: return@launchUI

        val chaptersNotDownloaded = async {
            val comicDir = provider.findComicDirectory(comic, source)

            chaptersToDownload
                    .distinctBy { it.name }
                    .filter { isChapterNotDownloaded(it, comicDir) }
                    .sortedByDescending { it.sourceOrder }
        }

        val chaptersToQueue = chaptersNotDownloaded
                                                        .await()
                                                        .filter { chapter -> filterChaptersAlreadyEnqueued(queue, chapter) }
                                                        .map { Download(source, comic, it) }

        if (chaptersToQueue.isNotEmpty()) {
            queue.addAll(chaptersToQueue)

            downloadNotification.initialQueueSize = queue.size

            if (isRunning) relay.accept(chaptersToQueue)
            if (autoStart) DownloaderService.startDownloaderService(this@Downloader.context)
        }
    }

    private fun isChapterNotDownloaded(chapter: Chapter, comicDir: UniFile?): Boolean {
        return comicDir?.findFile(provider.getChapterDirectoryName(chapter)) == null
    }

    private fun filterChaptersAlreadyEnqueued(queue: DownloadQueueList, chapter: Chapter): Boolean {
        return queue.none { it.chapter.id == chapter.id }
    }

    private fun initializeSubscriptions() {
        if (isRunning) return
        isRunning = true
        runningRelay.accept(true)

        subscriptions.clear()

        subscriptions.add(relay
                .concatMapIterable { it }
                .concatMap { downloadChapter(it).subscribeOn(Schedulers.io()) }
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ completeDownload(it)
                }, { error ->
                    DownloaderService.stopDownloaderService(context)
                    downloadNotification.onError(error.message)
                }))
    }

    private fun onDestroyAllSubscriptions() {
        if (!isRunning) return

        subscriptions.dispose()
        isRunning = false
        runningRelay.accept(false)
    }

    private fun downloadChapter(download: Download): Observable<Download> {
        return Observable.defer {
            val chapterDirName = provider.getChapterDirectoryName(download.chapter)

            val comicDir = provider.getComicDirectory(download.comic, download.source)
            val tempDir = comicDir?.createDirectory("${chapterDirName}_tmp")

            val pageListObservable = if (download.chapter.pages == null) {
                download.source.fetchPageList(download.chapter)
                        .doOnNext{ pages ->
                            if (pages.isEmpty())
                                throw Exception("Page list is empty")

                            download.chapter.pages = pages
                        }
            } else {
                Observable.just(download.chapter.pages)
            }

            pageListObservable
                    .doOnNext { _ ->
                        tempDir?.listFiles()
                                    ?.filter { it.name!!.endsWith(".tmp") }
                                    ?.forEach { it.delete() }

                            download.numberOfImagesDownloaded = 0
                            download.status = Download.DOWNLOADING
                        }
                        .flatMap { download.source.fetchAllImageUrlsFromPageList(it) }
                        .concatMap { page -> getOrDownloadPage(page, download, tempDir) }
                        .doOnNext { page: Page? -> downloadNotification.onDownloadProgressChange(download) }
                        .map { _ -> download }
                        .doOnNext { onCheckDownloads(download, comicDir, tempDir, chapterDirName) }
                        .onErrorReturn { error ->
                            download.status = Download.ERROR
                            downloadNotification.onError(error.message, download.chapter.name)
                            download
                        }
        }
    }

    private fun getOrDownloadPage(page: Page, download: Download, tmpDirectory: UniFile?): Observable<Page>? {
        if (page.imageUrl.isNullOrEmpty()) return Observable.just(page)

        val pageFileName = String.format("page_%03d", page.index)
        val tmpPageFile = tmpDirectory?.findFile("$pageFileName.tmp")

        tmpPageFile?.delete()

        val pageFile = tmpDirectory!!.listFiles()!!.find { page -> page.name!!.startsWith("$pageFileName") }

        val pageObservable = if (pageFile != null)
            Observable.just(pageFile)
        else
            downloadImage(page, download.source, tmpDirectory, pageFileName)

        return pageObservable
                .doOnNext{ file: UniFile ->
                    page.uri = file.uri
                    page.progress = 100
                    page.status = Page.READY
                    download.numberOfImagesDownloaded++
                }
                .map { page }
                .onErrorReturn {
                    page.progress = 0
                    page.status = Page.ERROR
                    page
                }
    }

    private fun downloadImage(page: Page, httpSource: HttpSourceBase?, tmpDirectory: UniFile?, pageFileName: String): Observable<UniFile?> {
        page.status = Page.DOWNLOAD_IMAGE
        page.progress = 0

        return httpSource!!.fetchImage(page)
                .map { response: Response ->
                    val file = tmpDirectory?.createFile("$pageFileName.tmp")

                    try {
                        response.body()!!.source().saveTo(file!!.openOutputStream())

                        val extension = getImageExtension(response, file)
                        file.renameTo("$pageFileName.$extension")
                    } catch (e: Exception) {
                        response.close()
                        file!!.delete()
                        throw e
                    }

                    file
                }
                .retryWhen(RetryWithDelay(3, { (2 shl it - 1) * 1000 }, Schedulers.trampoline()))
    }

    private fun onCheckDownloads(download: Download, comicDir: UniFile?, tempDir: UniFile?, chapterDirName: String) {
        val downloadedImages = tempDir!!.listFiles().orEmpty().filterNot { it.name!!.endsWith(".tmp") }

        download.status = if (downloadedImages.size == download.chapter.pages?.size)
            Download.DOWNLOADED
        else
            Download.ERROR

        if (download.status == Download.DOWNLOADED) {
            tempDir.renameTo(chapterDirName)
            cache.addChapter(chapterDirName, comicDir!!, download.comic)
        }
    }

    private fun getImageExtension(response: Response, file: UniFile?): String {
        val mime = response.body()?.contentType()?.let { ct -> "${ct.type()}/${ct.subtype()}" }
                            ?: context.contentResolver.getType(file!!.uri)
                            ?: DiskUtils.findImageMime { file!!.openInputStream() }

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

    }

    private fun completeDownload(download: Download) {
        if (download.status == Download.DOWNLOADED)
            queue.remove(download)

        if (areAllDownloadsFinished()) {
            if (!downloadNotification.isErrorThrow)
                downloadNotification.onDownloadCompleted(download, queue)

            DownloaderService.stopDownloaderService(context)
        }
    }

    private fun areAllDownloadsFinished(): Boolean {
        return queue.none { it.status == Download.DOWNLOADING || it.status == Download.ERROR }
    }
}