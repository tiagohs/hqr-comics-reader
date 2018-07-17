package com.tiagohs.hqr.ui.presenter

import android.Manifest
import android.content.Context
import android.webkit.MimeTypeMap
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.helpers.extensions.hasPermission
import com.tiagohs.hqr.helpers.extensions.saveTo
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.RetryWithDelay
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.ReaderChapterViewModel
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.Response
import timber.log.Timber

class ReaderPresenter(
        private val preferenceHelper: PreferenceHelper,
        private val sourceManager: SourceManager,
        private val comicsRepository: IComicsRepository,
        private val downloadManager: DownloadManager,
        private val historyRepository: IHistoryRepository,
        private val provider: DownloadProvider,
        private val context: Context
): BasePresenter<ReaderContract.IReaderView>(), ReaderContract.IReaderPresenter {

    private var tempDirectory: UniFile? = null

    private val downloadSubject = PublishSubject.create<List<Page>>()

    private var model: ReaderChapterViewModel? = null
    private var currentPage: Page? = null

    override fun onCreate() {

        mSubscribers.add(downloadSubject
                .observeOn(Schedulers.io())
                .flatMap {Observable.fromIterable(it) }
                .filter {it.status != Page.READY }
                .concatMap {
                    val sourceId = preferenceHelper.currentSource().getOrDefault()
                    val httpSource = sourceManager.get(sourceId)

                    getPageAndNotify(it, httpSource)
                }
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer()
                .toObservable()
                .subscribe({ mView?.onPageDownloaded(it)
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    override fun onUnbindView() {
        super.onUnbindView()

        if (model != null && !model!!.isDownloaded) {
            tempDirectory?.delete()
        }
    }

    override fun onGetChapterDetails(comicPath: String, chapterPath: String, updateDataSet: Boolean) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        mSubscribers.add(comicsRepository
                .findByPathUrl(comicPath, sourceId)
                .map { toModel(it.chapters?.find { it.chapterPath.equals(chapterPath) }!!, it) }
                .flatMap { onLoadChapter(sourceId, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    model = it
                    mView?.onBindChapter(it, updateDataSet)
                 }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    override fun onRequestNextChapter() {
        val chapterList = model?.comic?.chapters ?: return
        val chapter = model?.chapter ?: return

        var currChapterIndex = chapterList.indexOfFirst { chapter.id == it.id }
        val nextChapter = chapterList.getOrNull(--currChapterIndex)

        if (nextChapter != null) {

            mSubscribers.dispose()
            mSubscribers = CompositeDisposable()

            if (model != null && !model!!.isDownloaded) {
                tempDirectory?.delete()
            }

            onGetChapterDetails(model?.comic?.pathLink!!, nextChapter.chapterPath!!, true)
        }
    }

    override fun onTrackUserHistory(page: Page?) {
        this.currentPage = page;
    }

    override fun onSaveUserHistory() {

        if (currentPage != null) {
            val history = ComicHistoryViewModel().apply {
                this.chapter = model?.chapter
                this.comic = model?.comic
                this.lastTimeRead = DateUtils.getDateToday()

                this.chapter?.lastPageRead = currentPage!!.index
            }

            historyRepository.insertComicHistory(history)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({}, { error ->
                        Timber.e(error)
                        mView?.onError(error)
                    })
        }
    }

    private fun onLoadChapter(sourceId: Long, model: ReaderChapterViewModel): Observable<ReaderChapterViewModel> {
        val hasPermissionToWrite = context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val httpSource = sourceManager.get(sourceId)

        if (model.isDownloaded) {
            return downloadManager.buildListOfPages(model.comic.source!!, model.comic, model.chapter)
                                .map{
                                    model.pages = it

                                    model
                                }
        } else {
            if (hasPermissionToWrite) {
                val chapterDirName = provider.findChapterDirectory(model.chapter, model.comic, model.comic.source!!) ?: provider.getChapterDirectoryName(model.chapter)
                val comicDir = provider.findComicDirectory(model.comic, model.comic.source!!) ?: provider.getComicDirectory(model.comic, model.comic.source!!)

                tempDirectory = comicDir?.findFile("${chapterDirName}_tmp")

                if (tempDirectory == null) {
                    tempDirectory = comicDir?.createDirectory("${chapterDirName}_tmp")
                } else {
                    tempDirectory?.listFiles()?.forEach { it.delete() }
                }
            }

            return httpSource!!.fetchPageList(model.chapter)
                    .map{
                        val pages = ArrayList<Page>()

                        pages.addAll(it)
                        pages.add(Page(it.lastIndex + 1, "", "", null, true))

                        model.pages = pages

                        if (hasPermissionToWrite) downloadSubject.onNext(it)

                        model
                    }
        }
    }

    private fun getPageAndNotify(page: Page, httpSource: IHttpSource?): Observable<Page> {
        val pageFileName = String.format("page_%03d", page.index)

        return downloadImage(page, httpSource, pageFileName)
                .map {
                    page.uri = it.uri
                    page.status = Page.READY

                    page
                }
    }

    private fun downloadImage(page: Page, httpSource: IHttpSource?, pageFileName: String): Observable<UniFile?> {
        page.status = com.tiagohs.hqr.models.sources.Page.DOWNLOAD_IMAGE
        page.progress = 0

        return httpSource!!.fetchImage(page)
                .map { response: Response ->
                    val file = tempDirectory?.createFile("$pageFileName.tmp")

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

    private fun getImageExtension(response: Response, file: UniFile?): String {
        val mime = response.body()?.contentType()?.let { ct -> "${ct.type()}/${ct.subtype()}" }
                ?: context.contentResolver.getType(file!!.uri)
                ?: DiskUtils.findImageMime { file!!.openInputStream() }

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

    }

    private fun toModel(chapter: ChapterViewModel, comic: ComicViewModel): ReaderChapterViewModel {
        return ReaderChapterViewModel(chapter, comic).apply {
            isDownloaded = downloadManager.isChapterDownloaded(chapter, comic, true)
        }
    }

}