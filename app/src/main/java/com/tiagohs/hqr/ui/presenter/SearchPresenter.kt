package com.tiagohs.hqr.ui.presenter

import android.util.Log
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
                .subscribe({ comic ->
                    Log.d("LIST_SEARCH", "Inicialização: " + comic?.comic?.name)

                    mView?.onBindItem(comic!!)
                }, { error ->
                    Log.e("LIST_SEARCH", "Inicialização Falhou ", error)
                }))
    }

    override fun onSearchComics(query: String) {

        mSubscribers.add(interceptor.onSearchComics(query)
                .map { it.map { it.toModel() } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comicsList -> mView!!.onBindComics(comicsList) },
                        { error -> Log.e("Search", "Error", error) }
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
                            { error -> Log.e("List", "Error", error) }
                    ))
        }
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        comicRepository.addOrRemoveFromFavorite(comic, sourceId)
                .subscribeOn(Schedulers.io())
                .map { it.toModel() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mView?.onBindItem(it) }
    }

    private fun ComicViewModel.toModel(): ComicItem {
        return ComicItem(this, R.layout.item_comic_simple_it)
    }

}