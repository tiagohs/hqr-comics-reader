package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.*
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
    var hasInAllPageSupport = false
    var hasInScanlatorPageSupport = false
    var hasInGenresPageSupport = false
    var hasInPublisherPageSupport = false

    var type: String = ""

    var hasMorePagesFromNetwork: Boolean = false
    var page: Int = 0
    var originalList: ArrayList<ComicViewModel> = ArrayList()

    override fun onGetAllByLetter(letter: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        type = FETCH_ALL

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByLetter(letter, page++)!!, sourceHttp, sourceId)
    }

    override fun onGetAllByScanlator(scanlator: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        type = FETCH_BY_SCANLATORS

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByScanlator(scanlator)!!, sourceHttp, sourceId)
    }

    override fun onGetAllByPublisher(publisher: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        type = FETCH_BY_PUBLISHERS

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByPublisher(publisher, page++)!!, sourceHttp, sourceId)
    }

    override fun onGetAllByGenres(genre: String): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        type = FETCH_BY_GENRES

        return onGetAllByFlag(sourceHttp?.fetchAllComicsByGenre(genre, page++)!!, sourceHttp, sourceId)
    }

    private fun onGetAllByFlag(fetcher: Observable<List<ComicViewModel>>, sourceHttp: IHttpSource, sourceId: Long): Observable<List<ComicViewModel>> {
        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    hasInAllPageSupport = it.hasInAllPageSupport
                    hasInScanlatorPageSupport = it.hasInScanlatorPageSupport
                    hasInGenresPageSupport = it.hasInGenresPageSupport
                    hasInPublisherPageSupport = it.hasInPublisherPageSupport

                    onGetComics( fetcher, comicsRepository.findAll(sourceId), null, it, sourceHttp, null, ALL) }
                .map { comics ->
                    var localComics = emptyList<ComicViewModel>()

                    if (!getIfHasPageSupport()) {
                        listPaginator = ListPaginator()
                        localComics = listPaginator!!.onCreatePagination(comics)
                    } else {
                        localComics = comics

                        hasMorePagesFromNetwork = localComics.isNotEmpty()
                    }

                    comicsRepository.insertRealm(localComics, sourceId)!!
                }
                .doOnNext { comics ->

                    if (originalList.isNotEmpty()) {
                        var hasComics: Boolean = false

                        comics.forEach { item ->
                            val value = originalList.find { original ->
                                original.pathLink.equals(item.pathLink)
                            }

                            if (value != null) {
                                hasComics = true
                            }
                        }

                        if (!hasComics) {
                            originalList.addAll(comics)
                            initializeComics(comics)

                            hasMorePagesFromNetwork = comics.isNotEmpty()
                        } else {
                            hasMorePagesFromNetwork = false
                        }
                    } else {
                        originalList.addAll(comics)
                        initializeComics(comics)

                        hasMorePagesFromNetwork = comics.isNotEmpty()
                    }
                }
    }

    override fun onGetMore(flag: String): Observable<List<ComicViewModel>> {

        if (getIfHasPageSupport()) {
            val observable: Observable<List<ComicViewModel>> = if (type == FETCH_ALL) {
                onGetAllByLetter(flag)
            } else if (type == FETCH_BY_PUBLISHERS) {
                onGetAllByPublisher(flag)
            }else if (type == FETCH_BY_GENRES) {
                onGetAllByGenres(flag)
            }else if (type == FETCH_BY_SCANLATORS) {
                onGetAllByScanlator(flag)
            } else {
                onGetAllByLetter(flag)
            }

            return observable
                    .map { comics ->
                            val sourceId = preferenceHelper.currentSource().getOrDefault()
                        comicsRepository.insertRealm(comics, sourceId)!!
                    }
                    .doOnNext { comics -> initializeComics(comics) }
                    .map {

                        originalList
                    }
        }

        return listPaginator!!.onGetNextPage()
                            .map { comics ->
                                    val sourceId = preferenceHelper.currentSource().getOrDefault()
                                    comicsRepository.insertRealm(comics, sourceId)!!
                                }
                                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun hasPageSuport(): Boolean {
        return getIfHasPageSupport()
    }

    override fun hasMoreComics(): Boolean {
        return listPaginator?.hasMorePages ?: hasMorePagesFromNetwork
    }

    override fun getOriginalList(): List<ComicViewModel> {
        return listPaginator?.originalList!!
    }

    private fun getIfHasPageSupport(): Boolean {

        return when (type) {
            FETCH_ALL -> hasInAllPageSupport
            FETCH_BY_PUBLISHERS -> hasInPublisherPageSupport
            FETCH_BY_GENRES -> hasInGenresPageSupport
            FETCH_BY_SCANLATORS -> hasInScanlatorPageSupport
            else -> hasInAllPageSupport
        }
    }

}