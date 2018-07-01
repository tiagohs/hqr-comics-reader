package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class HomeInterceptor(
    private val preferenceHelper: PreferenceHelper,
    private val comicsRepository: IComicsRepository,
    private val sourceRepository: ISourceRepository,
    private val sourceManager: SourceManager
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager),
        Contracts.IHomeInterceptor {

    override fun onGetLastestComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    val observable = onGetComics(
                            sourceHttp?.fetchLastestComics()!!,
                            comicsRepository.getRecentsComics(sourceId),
                            it.lastLastestUpdate,
                            it,
                            sourceHttp,
                            IComic.RECENTS)

                    it.lastLastestUpdate = DateUtils.getDateToday()
                    sourceRepository.insertSource(it).subscribe()

                    observable
                }
                .doOnNext { comics -> initializeComics(comics) }
    }

    override fun onGetPopularComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    val observable = onGetComics(
                            sourceHttp?.fetchPopularComics()!!,
                            comicsRepository.getPopularComics(sourceId),
                            it.lastPopularUpdate,
                            it,
                            sourceHttp,
                            IComic.POPULARS)

                    it.lastPopularUpdate = DateUtils.getDateToday()
                    sourceRepository.insertSource(it).subscribe()

                    observable
                }
                .doOnNext { comics -> initializeComics(comics) }
    }

}