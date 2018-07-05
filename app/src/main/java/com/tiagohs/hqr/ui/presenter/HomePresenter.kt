package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.R
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class HomePresenter(
        private val sourceManager: SourceManager,
        private val preferenceHelper: PreferenceHelper,
        private val sourceRepository: ISourceRepository,
        private val homeInterceptor: Contracts.IHomeInterceptor,
        private val comicRepository: IComicsRepository
): BasePresenter<HomeContract.IHomeView>(), HomeContract.IHomePresenter {

    override fun onBindView(view: HomeContract.IHomeView) {
        super.onBindView(view)

        homeInterceptor.onBind()
        homeInterceptor.subscribeComicDetailSubject()
                        .map { it.toModel() }
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe({ comic ->
                             Log.d("HOME", "Inicialização: " + comic?.comic?.name)

                             if (comic?.comic?.tags != null) {
                                 if (comic.comic.tags!!.contains(IComic.POPULARS)) {
                                     mView?.onBindPopularItem(comic)
                                 }
                                 if (comic.comic.tags!!.contains(IComic.RECENTS)) {
                                     mView?.onBindLastestItem(comic)
                                 }
                             }

                         }, { error ->
                             Log.e("Home", "Inicialização Falhou ", error)
                         })
    }

    override fun onUnbindView() {
        super.onUnbindView()

        homeInterceptor.onUnbind()
    }

    override fun onGetHomeData(sourceId: Long) {
        sourceRepository.getSourceById(sourceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ source: SourceDB? ->
                    val sourceHttp = sourceManager.get(sourceId) as HttpSourceBase?

                    mView?.onBindSourceInfo(source!!)

                    if (sourceHttp != null) {
                        onGetPublishers(sourceHttp)
                        onGetHomePageData(sourceHttp)
                    }

                })
    }

    override fun onGetPublishers(source: HttpSourceBase) {

        mSubscribers.add(source.fetchPublishers()
                .map({ publishers -> publishers.subList(1, publishers.size) })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { publishers: List<Publisher>? ->
                            if (publishers!!.isNotEmpty()) {
                                mView!!.onBindPublishers(publishers)
                            }
                        },
                        { error: Throwable? ->
                            Log.e("HomePresenter", "Error!", error)
                        }))


    }

    override fun onGetHomePageData(source: HttpSourceBase) {

        mSubscribers.add(homeInterceptor.onGetPopularComics()
                .map { it.map { it.toModel() } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ popularComics ->
                    if (popularComics != null) mView!!.onBindPopulars(popularComics)
                }, { error ->
                    Log.e("HomePresenter", "Error!", error)
                }))

        mSubscribers.add(homeInterceptor.onGetLastestComics()
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.map { it.toModel() } }
                .subscribe({ lastestsComics ->
                    if (lastestsComics != null) mView!!.onBindLastestUpdates(lastestsComics)
                }, { error ->
                    Log.e("HomePresenter", "Error!", error)
                }))
    }

    override fun observeSourcesChanges() {
        preferenceHelper.currentSource()
                .asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ sourceId: Long? ->
                    onGetHomeData(sourceId!!)
                })
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        comicRepository.addOrRemoveFromFavorite(comic, sourceId)
                .subscribeOn(Schedulers.io())
                .map { it.toModel() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { comic ->
                    if (comic?.comic?.tags != null) {
                        if (comic.comic.tags!!.contains(IComic.POPULARS)) {
                            mView?.onBindPopularItem(comic)
                        }
                        if (comic.comic.tags!!.contains(IComic.RECENTS)) {
                            mView?.onBindLastestItem(comic)
                        }
                    }
                }
    }

    private fun ComicViewModel.toModel(): ComicItem {
        return ComicItem(this, R.layout.item_comic)
    }

}