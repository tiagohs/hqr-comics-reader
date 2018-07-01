package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.FETCH_ALL
import com.tiagohs.hqr.models.view_models.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.view_models.FETCH_BY_SCANLATORS
import com.tiagohs.hqr.ui.contracts.ListComicsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ListComicsPresenter(
        private val interceptor: Contracts.IListComicsInterceptor
): BasePresenter<ListComicsContract.IListComicsView>(), ListComicsContract.IListComicsPresenter {

    override fun onBindView(view: ListComicsContract.IListComicsView) {
        super.onBindView(view)

        interceptor.onBind()
        mSubscribers?.add(interceptor.subscribeComicDetailSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comic: ComicViewModel? ->
                    Log.d("LIST_COMICS", "Inicialização: " + comic?.name)

                    mView?.onBindItem(comic!!)
                }, { error ->
                    Log.e("LIST_COMICS", "Inicialização Falhou ", error)
                }))
    }

    override fun onGetComics(listType: String, flag: String) {

        mSubscribers!!.add(onCheckListType(listType, flag)
              .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { mView!!.onBindComics(it) },
                        { error -> Log.e("List", "Error", error) }
                ))
    }

    override fun hasPagesSupport(): Boolean {
        return interceptor.hasPageSuport()
    }

    override fun getOriginalList(): List<ComicViewModel> {
        return interceptor.getOriginalList()
    }

    fun onCheckListType(listType: String, flag: String): Observable<List<ComicViewModel>> {

        when (listType) {
            FETCH_ALL -> return interceptor.onGetAllByLetter(flag)
            FETCH_BY_PUBLISHERS -> return interceptor.onGetAllByPublisher(flag)
            FETCH_BY_SCANLATORS -> return interceptor.onGetAllByScanlator(flag)
        }

        return interceptor.onGetAllByLetter(flag)
    }

    override fun hasMoreComics(): Boolean {
        return interceptor.hasMoreComics()
    }

    override fun onGetMoreComics() {

        if (interceptor.hasMoreComics()) {
            mSubscribers!!.add(interceptor.onGetMore()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindMoreComics(it) },
                            { error -> Log.e("List", "Error", error) }
                    ))
        }

    }

}