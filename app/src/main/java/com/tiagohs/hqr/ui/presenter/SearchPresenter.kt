package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.models.viewModels.ComicsListViewModel
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.SearchContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.collections4.ListUtils

class SearchPresenter(
    private val subscriber: CompositeDisposable,
    private val source: HQBRSource
): BasePresenter<SearchContract.ISearchView>(subscriber), SearchContract.ISearchPresenter {

    var originalComicsList: List<ComicViewModel>? = null
    var comicsListsByPage: List<List<ComicViewModel>> = ArrayList()
    var comicsList: List<ComicViewModel> = ArrayList()
    var hasMorePages: Boolean = false
    var hasPagesSupport: Boolean = false
    var currentPage: Int = 0
    var totalPage: Int = 0

    override fun onSearchComics(query: String) {

        mSubscribers!!.add(source.fetchSearchByQuery(query)
                .observeOn(AndroidSchedulers.mainThread())
                .map({ model: ComicsListViewModel ->
                    if (model.hasPagesSupport) {
                        hasPagesSupport = model.hasPagesSupport
                        originalComicsList = model.comics
                        model.comics
                    } else
                        onCreatePagination(model.comics) })
                .subscribe({ comicsList ->
                    this.comicsList = comicsList
                    mView!!.onBindComics(comicsList)
                }))
    }

    override fun hasMoreComics(): Boolean {
        return hasMorePages
    }

    override fun hasPagesSupport(): Boolean {
        return hasPagesSupport
    }

    override fun onGetMoreComics() {

        if (hasMorePages) {
            mSubscribers!!.add(onGetNextPageComics()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ comics ->
                        comicsList = comics

                        mView!!.onBindComics(comics)
                    }))
        }
    }

    private fun onCreatePagination(comicsList: List<ComicViewModel>): List<ComicViewModel> {

        if (comicsList.size > 0) {
            currentPage = 0
            comicsListsByPage = ListUtils.partition<ComicViewModel>(comicsList, 20)
            totalPage = comicsListsByPage.size
            hasMorePages = currentPage < totalPage - 1

            if (comicsListsByPage.size > 0)
                return comicsListsByPage.get(currentPage++)
            else
                return ArrayList()
        } else
            return ArrayList()
    }

    private fun onGetNextPageComics(): Observable<List<ComicViewModel>> {
        return Observable
                .create<List<ComicViewModel>>({ emitter ->
                    if (currentPage < totalPage - 1) {
                        hasMorePages = ++currentPage < totalPage - 1
                        val newComics = ListUtils.union(comicsList, comicsListsByPage.get(currentPage))

                        emitter.onNext(newComics)
                    }

                    emitter.onComplete()
                })
    }

}