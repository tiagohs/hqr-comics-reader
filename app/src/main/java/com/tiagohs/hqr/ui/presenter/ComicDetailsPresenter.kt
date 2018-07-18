package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class ComicDetailsPresenter(
        private val interceptor: Contracts.IComicsDetailsInterceptor,
        private val comicRepository: IComicsRepository,
        private val preferenceHelper: PreferenceHelper,
        private val historyRepository: IHistoryRepository
): BasePresenter<ComicDetailsContract.IComicDetailsView>(),
        ComicDetailsContract.IComicDetailsPresenter {

    private var history: ComicHistoryViewModel? = null

    override fun onGetComicData(comicPath: String, sourceId: Long) {
        mSubscribers.add(interceptor.onGetComicData(comicPath, sourceId)
                .doOnNext { this.history = historyRepository.findByComicIdRealm(it!!.id) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                      { comic: ComicViewModel? ->
                            if (comic != null) mView!!.onBindComic(comic, history)
                      },
                      { error ->
                          Timber.e(error)
                      }
                 ))
    }

    override fun getComicHistory(): ComicHistoryViewModel? {
        return history
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        mSubscribers.add(comicRepository.addOrRemoveFromFavorite(comic, sourceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onConfigureFavoriteBtn(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }
                ))
    }


}