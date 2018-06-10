package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class ComicDetailsPresenter(subscriber: CompositeDisposable,
                            private val source: HQBRSource):
        BasePresenter<ComicDetailsContract.IComicDetailsView>(subscriber),
        ComicDetailsContract.IComicDetailsPresenter {

    override fun onGetComicData(comicPath: String) {
        mSubscribers!!.add(source.fetchComicDetails(comicPath)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                      { comic: Comic? ->
                            if (comic != null) mView!!.onBindComic(comic)
                      }
              ))
    }

}