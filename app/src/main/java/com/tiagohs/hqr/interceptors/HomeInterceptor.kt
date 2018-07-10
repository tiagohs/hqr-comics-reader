package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IDefaultModelsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class HomeInterceptor(
    private val preferenceHelper: PreferenceHelper,
    private val comicsRepository: IComicsRepository,
    private val sourceRepository: ISourceRepository,
    private val sourceManager: SourceManager,
    private val defaultModelRepository: IDefaultModelsRepository
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager, sourceRepository),
        Contracts.IHomeInterceptor {

    var publishersPaginator: ListPaginator<DefaultModelView>? = null
    var popularPaginator: ListPaginator<ComicViewModel>? = null
    var lastestPaginator: ListPaginator<ComicViewModel>? = null

    override fun onGetLastestComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    onGetComics(
                            sourceHttp?.fetchLastestComics()!!,
                            comicsRepository.getRecentsComics(sourceId),
                            it.lastLastestUpdate,
                            it,
                            sourceHttp,
                            IComic.RECENTS)
                }
                .doOnNext {
                    val source = sourceRepository.getSourceByIdRealm(sourceId)

                    if (source != null) {
                        source.lastLastestUpdate = DateUtils.getDateToday()
                        sourceRepository.insertSource(source).subscribe()
                    }
                }
                .map { comics ->
                    lastestPaginator = ListPaginator()

                    lastestPaginator!!.onCreatePagination(comics, 12)
                }
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun onGetPopularComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    onGetComics(
                            sourceHttp?.fetchPopularComics()!!,
                            comicsRepository.getPopularComics(sourceId),
                            it.lastPopularUpdate,
                            it,
                            sourceHttp,
                            IComic.POPULARS)
                }
                .doOnNext {
                    val source = sourceRepository.getSourceByIdRealm(sourceId)

                    if (source != null) {
                        source.lastPopularUpdate = DateUtils.getDateToday()
                        sourceRepository.insertSource(source).subscribe()
                    }
                }
                .map { comics ->
                    popularPaginator = ListPaginator()

                    popularPaginator!!.onCreatePagination(comics, 12)
                }
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun onGetPublishers(): Observable<List<DefaultModelView>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return defaultModelRepository.getAllPublishers(sourceId)
                .flatMap {
                    if (it.isNotEmpty())
                        Observable.just(it)
                    else
                        fetchFromNetwork(sourceHttp, sourceId)
                }
                .map { publishers ->
                    publishersPaginator = ListPaginator()

                    publishersPaginator!!.onCreatePagination(publishers, 20)
                }
    }

    private fun fetchFromNetwork(sourceHttp: IHttpSource?, sourceId: Long): Observable<List<DefaultModelView>> {
        return sourceHttp?.fetchPublishers()!!
                .map { defaultModelRepository.insertRealm(it, sourceId) }
    }

    override fun onGetMorePublishers(): Observable<List<DefaultModelView>> {
        return publishersPaginator!!.onGetNextPage()
    }

    override fun onGetMorePopularComics(): Observable<List<ComicViewModel>> {
        return popularPaginator!!.onGetNextPage()
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun onGetMoreLastestComics(): Observable<List<ComicViewModel>> {
        return lastestPaginator!!.onGetNextPage()
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun hasMorePublishers(): Boolean {
        return publishersPaginator?.hasMorePages ?: false
    }

    override fun hasMorePopularComics(): Boolean {
        return popularPaginator?.hasMorePages ?: false
    }

    override fun hasMoreLastestComics(): Boolean {
        return lastestPaginator?.hasMorePages ?: false
    }

}