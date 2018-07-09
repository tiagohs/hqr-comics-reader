package com.tiagohs.hqr.interceptors

import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.IHttpSource
import com.tiagohs.hqr.sources.SourceManager
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class ComicsDetailsInterceptor(
        private val preferenceHelper: PreferenceHelper,
        private val comicsRepository: IComicsRepository,
        private val sourceRepository: ISourceRepository,
        private val sourceManager: SourceManager
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager, sourceRepository), Contracts.IComicsDetailsInterceptor {

    override fun onGetComicData(comicPath: String): Observable<ComicViewModel?> {
        val sourceId = preferenceHelper.currentSource().getOrDefault()
        val sourceHttp = sourceManager.get(sourceId)

        return sourceRepository.getSourceById(sourceId)
                .observeOn(Schedulers.io())
                .flatMap { comicsRepository.findByPathUrl(comicPath, sourceId) }
                .concatMap {
                    if (!it.inicialized) {
                        fetchFromNetwork(sourceHttp!!, comicPath, sourceId)
                    } else {
                        Observable.just(it)
                    }
                }
    }

    private fun fetchFromNetwork(sourceHttp: IHttpSource, comicPath: String, sourceId: Long): Observable<ComicViewModel?> {
        return sourceHttp.fetchComicDetails(comicPath)
                .doOnNext {
                    it.inicialized = true
                    comicsRepository.insertRealm(it, sourceId)
                }
    }

}