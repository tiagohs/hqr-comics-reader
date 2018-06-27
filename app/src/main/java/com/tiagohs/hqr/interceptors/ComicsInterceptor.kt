package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.interceptors.config.BaseInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import com.tiagohs.hqr.models.sources.Comic as NetworkComic
import com.tiagohs.hqr.models.sources.ComicsItem as NetworkComicItem

class ComicsInterceptor(
    private val preferenceHelper: PreferenceHelper,
    private val comicsRepository: IComicsRepository,
    private val sourceRepository: ISourceRepository,
    private val sourceManager: SourceManager
): BaseInterceptor(), Contracts.IComicsInterceptor {

    private val comicDetailSubject = PublishSubject.create<List<ComicViewModel>>()

    override fun subscribeComicDetailSubject(): Observable<ComicViewModel> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return comicDetailSubject
                .observeOn(Schedulers.io())
                .flatMap {Observable.fromIterable(it) }
                .filter {it.posterPath.isNullOrEmpty() && !it.inicialized }
                .concatMap { comic: ComicViewModel -> getComicDetailsObservable(comic, sourceId, sourceHttp!!) }
                .toFlowable(BackpressureStrategy.LATEST)
                .onBackpressureBuffer()
                .toObservable()
    }

    override fun onGetLastestComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    onGetComics(
                            sourceHttp?.fetchLastestComics()!!,
                            comicsRepository.getRecentsComics(),
                            it.lastLastestUpdate,
                            it,
                            sourceHttp,
                            IComic.RECENTS)
                }
    }

    override fun onGetPopularComics(): Observable<List<ComicViewModel>> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap {
                    onGetComics(
                            sourceHttp?.fetchPopularComics()!!,
                            comicsRepository.getPopularComics(),
                            it.lastLastestUpdate,
                            it,
                            sourceHttp,
                            IComic.POPULARS)
                }
    }

    private fun onGetComics(networkFetcher: Observable<List<NetworkComicItem>>, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, source: ISource, sourceHttp: IHttpSource, type: String): Observable<List<ComicViewModel>> {
        return fromNetworkOrLocal(networkFetcher, source, sourceHttp, localFetcher, lastUpdate, type)
                            .doOnNext { comics -> initializeComics(comics) }
    }

    private fun fromNetworkOrLocal(networkFetcher: Observable<List<NetworkComicItem>>, source: ISource, httpSource: IHttpSource, localFetcher: Observable<List<ComicViewModel>>, lastUpdate: String?, type: String): Observable<List<ComicViewModel>> {
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

    private fun fetchComicsFromLocal(source: ISource, localFetcher: Observable<List<ComicViewModel>>, type: String): Observable<List<ComicViewModel>> {
        return localFetcher
                .doOnNext { onUpdateCatalogueSource(source, type) }
    }

    private fun fetchComicsFromNetwork(networkFetcher: Observable<List<NetworkComicItem>>, source: ISource, sourceHttp: IHttpSource, type: String): Observable<List<ComicViewModel>> {
        return networkFetcher
                         .map { networkComics -> networkComics.map { networkToLocalComic(it, sourceHttp.id, type) } }
                         .doOnNext { onUpdateCatalogueSource(source, type) }
    }

    private fun networkToLocalComic(comicNetwork: NetworkComicItem, sourceId: Long, type: String) : ComicViewModel {
        var comic = comicsRepository.getComicRealm(comicNetwork.link, sourceId)

        if (comic == null) {
            var tags: List<String>? = null

            when(type) {
                IComic.POPULARS -> tags = listOf(IComic.POPULARS)
                IComic.RECENTS -> tags = listOf(IComic.RECENTS)
            }

            val newComic = comicsRepository.insertRealm(comicNetwork, sourceId, tags, false)
            comic = newComic
        }

        return ComicViewModel().create(comic!!)
    }

    private fun onUpdateCatalogueSource(currentSource: ISource, type: String) {
        val s = SourceDB()
        s.copyFrom(currentSource)

        when(type) {
            IComic.POPULARS -> s.lastPopularUpdate = DateUtils.getDateToday()
            IComic.RECENTS -> s.lastLastestUpdate = DateUtils.getDateToday()
        }

        sourceRepository.insertSource(s).subscribe()
    }

    private fun initializeComics(comics: List<ComicViewModel>) {
        comicDetailSubject.onNext(comics)
    }

    private fun getComicDetailsObservable(comic: ComicViewModel, sourceId: Long, httpSource: IHttpSource): Observable<ComicViewModel> {
        return httpSource.fetchComicDetails(comic.pathLink!!)
                         .flatMap { networkComic -> comicsRepository.insertOrUpdateComic(networkComic, sourceId) }
                         .doOnNext { Observable.just(it) }
    }
}