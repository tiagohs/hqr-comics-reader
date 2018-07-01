package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.contracts.SearchContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SearchPresenter(
    private val interceptor: Contracts.ISearchInterceptor
): BasePresenter<SearchContract.ISearchView>(), SearchContract.ISearchPresenter {

    override fun onBindView(view: SearchContract.ISearchView) {
        super.onBindView(view)

        interceptor.onBind()
        mSubscribers?.add(interceptor.subscribeComicDetailSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comic: ComicViewModel? ->
                    Log.d("LIST_SEARCH", "Inicialização: " + comic?.name)

                    mView?.onBindItem(comic!!)
                }, { error ->
                    Log.e("LIST_SEARCH", "Inicialização Falhou ", error)
                }))
    }

    override fun onSearchComics(query: String) {

        mSubscribers?.add(interceptor.onSearchComics(query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comicsList -> mView!!.onBindComics(comicsList) },
                        { error -> Log.e("Search", "Error", error) }
                ))
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
            mSubscribers?.add(interceptor.onGetMore()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindComics(it) },
                            { error -> Log.e("List", "Error", error) }
                    ))
        }
    }

}