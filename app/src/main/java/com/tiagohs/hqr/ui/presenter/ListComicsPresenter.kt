package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.viewModels.ComicsListModel
import com.tiagohs.hqr.models.viewModels.FETCH_ALL
import com.tiagohs.hqr.models.viewModels.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.viewModels.FETCH_BY_SCANLATORS
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.ListComicsContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.collections4.ListUtils

class ListComicsPresenter(
        val subscribers: CompositeDisposable,
        val source: HQBRSource
): BasePresenter<ListComicsContract.IListComicsView>(subscribers), ListComicsContract.IListComicsPresenter {

    var originalComicsList: List<ComicsItem> = ArrayList()
    var comicsListsByPage: List<List<ComicsItem>> = ArrayList()
    var comicsList: List<ComicsItem> = ArrayList()
    var hasMorePages: Boolean = false
    var hasPagesSupport: Boolean = false
    var currentPage: Int = 0
    var totalPage: Int = 0

    override fun onGetComics(listType: String, flag: String) {
        mSubscribers!!.add(onCheckListType(listType, flag)
              .observeOn(AndroidSchedulers.mainThread())
              .map({ model: ComicsListModel ->
                  if (model.hasPagesSupport) {
                      hasPagesSupport = model.hasPagesSupport
                      originalComicsList = model.comics
                      model.comics
                  } else
                      onCreatePagination(model.comics) })
                .subscribe({ comics: List<ComicsItem>? ->
                    comicsList = comics!!
                    mView!!.onBindComics(comicsList)}))
    }

    override fun hasPagesSupport(): Boolean {
        return hasPagesSupport
    }

    override fun getOriginalList(): List<ComicsItem> {
        return originalComicsList
    }

    fun onCheckListType(listType: String, flag: String): Observable<ComicsListModel> {

        when (listType) {
            FETCH_ALL -> return source.fetchAllComicsByLetter(flag)
            FETCH_BY_PUBLISHERS -> return source.fetchAllComicsByPublisher(flag)
            FETCH_BY_SCANLATORS -> return source.fetchAllComicsByScanlator(flag)
        }

        return source.fetchAllComicsByLetter(flag)
    }

    override fun hasMoreComics(): Boolean {
        return hasMorePages
    }

    override fun onGetMoreComics() {

        if (hasMorePages) {
            mSubscribers!!.add(onGetNextPageComics()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ comics ->
                        comicsList = comics

                        mView!!.onBindMoreComics(comics)
                    }))
        }

    }

    private fun onCreatePagination(comicsList: List<ComicsItem>): List<ComicsItem> {
        originalComicsList = comicsList
        comicsListsByPage = ListUtils.partition<ComicsItem>(comicsList, 20)
        totalPage = comicsListsByPage.size
        hasMorePages = currentPage < totalPage - 1

        return comicsListsByPage.get(currentPage++)
    }

    private fun onGetNextPageComics(): Observable<List<ComicsItem>> {
        return Observable
                .create<List<ComicsItem>>({ emitter ->
                    if (currentPage < totalPage - 1) {
                        hasMorePages = ++currentPage < totalPage - 1
                        val newComics = ListUtils.union(comicsList, comicsListsByPage.get(currentPage))

                        emitter.onNext(newComics)
                    }

                    emitter.onComplete()
                })
    }

}