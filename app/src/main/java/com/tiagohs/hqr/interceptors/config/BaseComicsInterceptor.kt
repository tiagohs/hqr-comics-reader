package com.tiagohs.hqr.interceptors.config

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

abstract class BaseComicsInterceptor(
        private val comicsRepository: IComicsRepository,
        private val preferenceHelper: PreferenceHelper,
        private val sourceManager: SourceManager,
        private val sourceRepository: ISourceRepository
): BaseInterceptor() {

    companion object {
        const val POPULAR = "POPULAR"
        const val LASTEST = "LASTEST"
        const val ALL = "ALL"
    }

    private val comicDetailSubject = PublishSubject.create<List<ComicViewModel>>()

    fun subscribeComicDetailSubject(): Observable<ComicViewModel> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return comicDetailSubject
                .observeOn(Schedulers.io())
                .flatMap {Observable.fromIterable(it) }
                .filter {it.posterPath.isNullOrEmpty() && !it.inicialized }
                .concatMap { comic: ComicViewModel -> getComicDetailsObservable(comic, sourceId, sourceHttp!!) }
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer()
                .toObservable()
    }

    fun onGetComics(networkFetcher: Observable<List<ComicViewModel>>, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, source: SourceDB, sourceHttp: IHttpSource, type: String? = null, searchType: String? = null): Observable<List<ComicViewModel>> {
        return fromNetworkOrLocal(networkFetcher, source, sourceHttp, localFetcher, lastUpdate, type, searchType)
    }

    fun initializeComics(comics: List<ComicViewModel>) {
        comicDetailSubject.onNext(comics)
    }

    private fun fromNetworkOrLocal(networkFetcher: Observable<List<ComicViewModel>>, source: SourceDB, httpSource: IHttpSource, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, type: String?, searchType: String? = null): Observable<List<ComicViewModel>> {
        return if (lastUpdate != null && !needToUpdate(lastUpdate))
            fetchComicsFromLocal(source, localFetcher, type)
        else
            fetchComicsFromNetwork(networkFetcher, source, httpSource, type, searchType)
    }

    private fun needToUpdate(lastUpdate: String): Boolean {
        val todayDate = DateUtils.formateStringToCalendar(DateUtils.getDateToday()).time
        val lastUpdateDate = DateUtils.formateStringToCalendar(lastUpdate).time
        val difference = Math.abs(todayDate.time - lastUpdateDate.time)

        return TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS) >= 2
    }

    private fun fetchComicsFromLocal(source: ISource, localFetcher: Observable<List<ComicViewModel>>, type: String?): Observable<List<ComicViewModel>> {
        return localFetcher
    }

    private fun fetchComicsFromNetwork(networkFetcher: Observable<List<ComicViewModel>>, source: SourceDB, sourceHttp: IHttpSource, type: String?, searchType: String? = null): Observable<List<ComicViewModel>> {
        return networkFetcher
                .map { networkComics ->
                    var listToInsert: List<ComicViewModel>? = networkComics.map { networkToLocalComic(it, type) }

                    if (type == IComic.POPULARS || type == IComic.RECENTS) {
                        listToInsert = comicsRepository.insertRealm(listToInsert!!, source.id)
                    }

                    listToInsert
                }
    }

    private fun networkToLocalComic(comicNetwork: ComicViewModel, type: String?) : ComicViewModel {
        var tags: List<String>? = null

        when(type) {
            IComic.POPULARS -> tags = listOf(IComic.POPULARS)
            IComic.RECENTS -> tags = listOf(IComic.RECENTS)
        }

        comicNetwork.tags = tags
        comicNetwork.inicialized = false

        return comicNetwork
    }

    private fun getComicDetailsObservable(comic: ComicViewModel, sourceId: Long, httpSource: IHttpSource): Observable<ComicViewModel> {
        return httpSource.fetchComicDetails(comic.pathLink!!)
                .flatMap { networkComic ->
                    networkComic.inicialized = true

                    comicsRepository.insertOrUpdateComic(networkComic, sourceId)
                }
                .doOnNext { Observable.just(it) }
    }

}