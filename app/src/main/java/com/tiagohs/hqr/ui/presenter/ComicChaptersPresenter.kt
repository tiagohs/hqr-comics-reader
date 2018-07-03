package com.tiagohs.hqr.ui.presenter

import com.jakewharton.rxrelay2.BehaviorRelay
import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.chapters.ChapterItem
import com.tiagohs.hqr.ui.contracts.ComicChaptersContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import eu.davidea.flexibleadapter.utils.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ComicChaptersPresenter(
    private val downloadManager: DownloadManager,
    private val chapterRepository: IChapterRepository
): BasePresenter<ComicChaptersContract.IComicChaptersView>(), ComicChaptersContract.IComicChaptersPresenter {

    var chapters: List<ChapterItem> = emptyList()
    lateinit var comicViewModel: ComicViewModel

    val chaptersRelay: BehaviorRelay<List<ChapterItem>> = BehaviorRelay.create()

    override fun onCreate(comic: ComicViewModel) {
        this.comicViewModel = comic

        chaptersRelay.toFlowable(BackpressureStrategy.BUFFER)
                    .onBackpressureBuffer()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ chapters ->
                        mView?.onNextChapters(chapters)
                    }, { error ->
                        Log.e("Chapters", "Error", error)
                    })

        chapterRepository.getAllChapters(comic.id)
                .map { chapters -> chapters.map { it.toModel() }}
                .doOnNext { chapters ->
                    setDownloadChapters(chapters, comic)

                    this.chapters = chapters

                    observeDownloads()
                }
                .subscribe { chaptersRelay.accept(it) }

    }

    fun setDownloadChapters(chapters: List<ChapterItem>, comic: ComicViewModel) {
        chapters.forEach {
            if (downloadManager.isChapterDownloaded(it.chapter, comic, true)) {
                it.status = Download.DOWNLOADED
            }
        }
    }

    fun observeDownloads() {
        mSubscribers.add(downloadManager.queue.getStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .filter { download -> download.comic.id.equals(comicViewModel.id) }
                .doOnNext { onDownloadStatusChange(it) }
                .subscribe( { download ->
                    mView?.onChapterStatusChange(download.chapter.toModel(), download.status)
                }, { error ->
                    Log.e("Chapters", "Error", error)
                }))
    }

    override fun downloadChapters(chapters: List<ChapterItem>) {
        downloadManager.downloadChapters(comicViewModel, chapters.map { it.toViewModel() })
    }

    override fun deleteChapters(chapters: List<ChapterItem>) {
        Observable.fromIterable(chapters)
                .doOnNext { deleteChapter(it) }
                .toList()
                .toObservable()
                .doOnNext { refreshChapters() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ },
                    { error -> mView?.onChapterDeletedError() },
                    { mView?.onChapterDeleted() })
    }

    private fun deleteChapter(chapterItem: ChapterItem) {
        val chapterViewModel = chapterItem.toViewModel()
        downloadManager.queue.remove(chapterViewModel)
        downloadManager.deleteChapter(chapterViewModel, comicViewModel, comicViewModel.source!!)

        chapterItem.status = Download.NOT_DOWNLOADED
        chapterItem.download = null
    }

    fun onDownloadStatusChange(download: Download) {

        if (download.status == Download.QUEUE) {
            chapters.find { it.chapter.id == download.chapter.id }?.let {
                if (it.download == null) {
                    it.download = download
                }
            }
        }

        if (download.status == Download.DOWNLOADED || download.status == Download.ERROR) {
            refreshChapters()
        }
    }

    private fun refreshChapters() {
        chaptersRelay.accept(chapters)
    }

    private fun ChapterViewModel.toModel(): ChapterItem {
        val model = ChapterItem(this, comicViewModel)
        val download = downloadManager.queue.find { it.chapter.chapterPath == chapterPath }

        if (download != null) {
            model.download = download
        }

        return model
    }

    private fun ChapterItem.toViewModel(): ChapterViewModel {
        val chapter = ChapterViewModel()
        chapter.copyFrom(this)

        return chapter
    }
}