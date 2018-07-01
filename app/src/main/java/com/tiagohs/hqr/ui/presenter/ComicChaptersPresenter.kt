package com.tiagohs.hqr.ui.presenter

import com.jakewharton.rxrelay2.BehaviorRelay
import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.contracts.ComicChaptersContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers

class ComicChaptersPresenter(
    private val downloadManager: DownloadManager,
    private val chapterRepository: IChapterRepository
): BasePresenter<ComicChaptersContract.IComicChaptersView>(), ComicChaptersContract.IComicChaptersPresenter {

    var chapters: List<ChapterViewModel> = emptyList()
    lateinit var comic: ComicViewModel

    val chaptersRelay: BehaviorRelay<List<ChapterViewModel>> = BehaviorRelay.create()

    override fun onCreate(comic: ComicViewModel) {
        this.comic = comic

        chapterRepository.getAllChapters(comic.id)
                .doOnNext { chapters ->
                    setDownloadChapters(chapters, comic)

                    this.chapters = chapters

                    observeDownloads()
                }
                .subscribe { chaptersRelay.accept(it) }

    }

    fun setDownloadChapters(chapters: List<ChapterViewModel>, comic: ComicViewModel) {
        chapters.forEach {
            if (downloadManager.isChapterDownloaded(it, comic)) {
                it.status = Download.DOWNLOADED
            }
        }
    }

    fun observeDownloads() {
        mSubscribers.add(downloadManager.queue.getStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .filter { download -> download.comic.id.equals(comic.id) }
                .doOnNext { onDownloadStatusChange(it) }
                .subscribe( { download ->

                }, { error ->

                }))
    }

    override fun downloadChapters(chapters: List<ChapterViewModel>) {
        downloadManager.downloadChapters(comic, chapters)
    }

    fun onDownloadStatusChange(download: Download?) {

    }


}