package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ComicDetailsPresenter(
        private val interceptor: Contracts.IComicsDetailsInterceptor,
        private val comicRepository: IComicsRepository,
        private val preferenceHelper: PreferenceHelper
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


    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        comicRepository.addOrRemoveFromFavorite(comic, sourceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mView?.onConfigureFavoriteBtn(it) }
    }


}