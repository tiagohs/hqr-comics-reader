package com.tiagohs.hqr.interceptors.config

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.base.ISource
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
        private val sourceManager: SourceManager
): BaseInterceptor() {

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

    fun onGetComics(networkFetcher: Observable<List<ComicViewModel>>, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, source: ISource, sourceHttp: IHttpSource, type: String? = null): Observable<List<ComicViewModel>> {
        return fromNetworkOrLocal(networkFetcher, source, sourceHttp, localFetcher, lastUpdate, type)
    }

    fun initializeComics(comics: List<ComicViewModel>) {
        comicDetailSubject.onNext(comics)
    }

    private fun fromNetworkOrLocal(networkFetcher: Observable<List<ComicViewModel>>, source: ISource, httpSource: IHttpSource, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, type: String?): Observable<List<ComicViewModel>> {
        return if (lastUpdate != null && !needToUpdate(lastUpdate))
            fetchComicsFromLocal(source, localFetcher, type)
        else
            fetchComicsFromNetwork(networkFetcher, source, httpSource, type)
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

    private fun fetchComicsFromNetwork(networkFetcher: Observable<List<ComicViewModel>>, source: ISource, sourceHttp: IHttpSource, type: String?): Observable<List<ComicViewModel>> {
        return networkFetcher
                .map { networkComics -> networkComics.map { networkToLocalComic(it, sourceHttp.id, type) } }
    }

    private fun networkToLocalComic(comicNetwork: ComicViewModel, sourceId: Long, type: String?) : ComicViewModel {
        var comic = comicsRepository.findByPathUrlRealm(comicNetwork.pathLink!!, sourceId)

        if (comic == null) {
            var tags: List<String>? = null

            when(type) {
                IComic.POPULARS -> tags = listOf(IComic.POPULARS)
                IComic.RECENTS -> tags = listOf(IComic.RECENTS)
            }

            comicNetwork.tags = tags
            comicNetwork.inicialized = false

            val newComic = comicsRepository.insertRealm(comicNetwork, sourceId)
            comic = newComic
        }

        return comic!!
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