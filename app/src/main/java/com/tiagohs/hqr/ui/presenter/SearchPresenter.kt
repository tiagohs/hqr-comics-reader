package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.R
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.contracts.SearchContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class SearchPresenter(
        private val interceptor: Contracts.ISearchInterceptor,
        private val comicRepository: IComicsRepository,
        private val preferenceHelper: PreferenceHelper
): BasePresenter<SearchContract.ISearchView>(), SearchContract.ISearchPresenter {

    override fun onBindView(view: SearchContract.ISearchView) {
        super.onBindView(view)

        interceptor.onBind()
        mSubscribers.add(interceptor.subscribeComicDetailSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.toModel() }
                .subscribe({ comic -> mView?.onBindItem(comic!!)
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    override fun onSearchComics(query: String) {

        mSubscribers.add(interceptor.onSearchComics(query)
                .map { it.map { it.toModel() } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comicsList -> mView!!.onBindComics(comicsList) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }
                ))
    }

    override fun onReset() {
        mSubscribers.dispose()
        mSubscribers = CompositeDisposable()
    }

    override fun hasMoreComics(): Boolean {
        return interceptor.hasMoreComics()
    }

    override fun hasPagesSupport(): Boolean {
        return interceptor.hasPageSuport()
    }

    override fun getOriginalList(): List<ComicViewModel> {
        return interceptor.getOriginalList()
    }

    override fun onGetMoreComics() {

        if (interceptor.hasMoreComics()) {
            mSubscribers.add(interceptor.onGetMore()
                    .subscribeOn(Schedulers.io())
                    .map { it.map { it.toModel() } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindMoreComics(it) },
                            { error ->
                                Timber.e(error)
                                mView?.onError(error)
                            }
                    ))
        }
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        mSubscribers.add(comicRepository.addOrRemoveFromFavorite(comic, sourceId)
                .subscribeOn(Schedulers.io())
                .map { it.toModel() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindItem(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }
                ))
    }

    private fun ComicViewModel.toModel(): ComicItem {
        this.favorite = comicRepository.isFavorite(pathLink!!)

        return ComicItem(this, R.layout.item_comic_simple_it)
    }

}