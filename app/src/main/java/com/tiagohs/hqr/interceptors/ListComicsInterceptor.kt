package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class ListComicsInterceptor(
        private val preferenceHelper: PreferenceHelper,
        private val comicsRepository: IComicsRepository,
        private val sourceRepository: ISourceRepository,
        private val sourceManager: SourceManager
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager, sourceRepository),
        Contracts.IListComicsInterceptor {

    var listPaginator: ListPaginator<ComicViewModel>? = null
    var hasPageSuport = false

    override fun onGetAllByLetter(letter: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByLetter(letter)!!, sourceHttp, sourceId)
    }

    override fun onGetAllByScanlator(scanlator: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByScanlator(scanlator)!!, sourceHttp, sourceId)
    }

    override fun onGetAllByPublisher(publisher: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByPublisher(publisher)!!, sourceHttp, sourceId)
    }

    private fun onGetAllByFlag(fetcher: Observable<List<ComicViewModel>>, sourceHttp: IHttpSource, sourceId: Long): Observable<List<ComicViewModel>> {
        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap { onGetComics( fetcher, comicsRepository.findAll(sourceId), it.lastAllComicsUpdate, it, sourceHttp, null, ALL) }
                .map { comics ->
                    listPaginator = ListPaginator()

                    if (!hasPageSuport) {
                        listPaginator!!.onCreatePagination(comics)
                    } else
                        comics
                }
                .doOnNext { comics ->
                    initializeComics(comics) }
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