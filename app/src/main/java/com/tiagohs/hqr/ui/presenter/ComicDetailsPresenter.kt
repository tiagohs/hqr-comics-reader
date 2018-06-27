package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.models.viewModels.ComicViewModel
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
                      { comic: ComicViewModel? ->
                            if (comic != null) mView!!.onBindComic(comic)
                      },
                      { error ->
                          Log.e("ComicDetails", "Error", error)
                      }
              ))
    }

}