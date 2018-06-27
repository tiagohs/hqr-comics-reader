package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.models.viewModels.ChapterViewModel
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class ReaderPresenter(
        subscribers: CompositeDisposable,
        private val source: HQBRSource
): BasePresenter<ReaderContract.IReaderView>(subscribers), ReaderContract.IReaderPresenter {

    override fun onGetChapterDetails(chapterPath: String, chapterName: String?) {
        mSubscribers!!.add(source.fetchReaderComics(chapterPath, chapterName)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ chapter: ChapterViewModel? -> mView!!.onBindChapter(chapter) },
                           { error: Throwable? -> Log.e("Reader", "Error", error) }))
    }
}