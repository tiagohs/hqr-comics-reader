package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers

class ComicDetailsPresenter(
        private val interceptor: Contracts.IComicsDetailsInterceptor
): BasePresenter<ComicDetailsContract.IComicDetailsView>(),
        ComicDetailsContract.IComicDetailsPresenter {

    override fun onGetComicData(comicPath: String) {
        mSubscribers!!.add(interceptor.onGetComicData(comicPath)
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