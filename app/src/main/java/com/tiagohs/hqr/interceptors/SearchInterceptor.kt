package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class SearchInterceptor(
        private val preferenceHelper: PreferenceHelper,
        private val comicsRepository: IComicsRepository,
        private val sourceRepository: ISourceRepository,
        private val sourceManager: SourceManager
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager, sourceRepository), Contracts.ISearchInterceptor {

    var listPaginator: ListPaginator<ComicViewModel>? = null
    var hasPageSuport = false

    override fun onSearchComics(query: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    hasPageSuport = it.hasPageSupport

                    if (it.localStorageUpdated) {
                        comicsRepository.searchComic(query, sourceId)
                    } else {
                        sourceHttp!!.fetchSearchByQuery(query)
                    }
                }
                .map { comics ->
                    listPaginator = ListPaginator()
                    val filterComics = comics.map {
                        val comicLocal = comicsRepository.findByPathUrlRealm(it.pathLink!!, sourceId)

                        if (comicLocal != null && it.inicialized)
                            comicLocal
                        else
                            it
                    }

                    if (!hasPageSuport) {
                        listPaginator!!.onCreatePagination(filterComics)
                    } else
                        filterComics
                }
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun onGetMore(): Observable<List<ComicViewModel>> {
        return listPaginator!!.onGetNextPage()
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun hasPageSuport(): Boolean {
        return hasPageSuport
    }

    override fun hasMoreComics(): Boolean {
        return listPaginator?.hasMorePages ?: false
    }

    override fun getOriginalList(): List<ComicViewModel> {
        return listPaginator?.originalList!!
    }

}