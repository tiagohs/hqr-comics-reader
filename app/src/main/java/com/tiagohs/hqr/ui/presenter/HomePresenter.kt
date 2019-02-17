package com.tiagohs.hqr.ui.presenter

import com.tiagohs.hqr.R
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.adapters.publishers.PublisherItem
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class HomePresenter(
        private val sourceManager: SourceManager,
        private val preferenceHelper: PreferenceHelper,
        private val sourceRepository: ISourceRepository,
        private val homeInterceptor: Contracts.IHomeInterceptor,
        private val comicRepository: IComicsRepository
): BasePresenter<HomeContract.IHomeView>(), HomeContract.IHomePresenter {

    private var soureObservable: Disposable? = null

    override fun onBindView(view: HomeContract.IHomeView) {
        super.onBindView(view)

        startInitializer()
    }

    private fun startInitializer() {
        homeInterceptor.onBind()
        mSubscribers.add(homeInterceptor.subscribeComicDetailSubject()
                .map { it.toModel() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comic ->

                    if (comic?.comic?.tags != null) {
                        if (comic.comic.tags!!.contains(IComic.POPULARS)) {
                            mView?.onBindPopularItem(comic)
                        }
                        if (comic.comic.tags!!.contains(IComic.RECENTS)) {
                            mView?.onBindLastestItem(comic)
                        }
                    }

                }, { error ->
                    Timber.e(error)
                }))
    }


    override fun onUnbindView() {
        super.onUnbindView()

        homeInterceptor.onUnbind()
        soureObservable?.dispose()
    }

    override fun onReset() {
        mSubscribers.dispose()
        mSubscribers = CompositeDisposable()

        startInitializer()
    }

    override fun observeSourcesChanges() {
        soureObservable = preferenceHelper.currentSource()
                .asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ sourceId: Long? ->
                    mView?.onReset()
                    onReset()
                    onGetHomeData(sourceId!!)
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                })
    }

    override fun onGetHomeData(sourceId: Long) {
        mSubscribers.add(sourceRepository.getSourceById(sourceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ source: SourceDB? ->
                    val sourceHttp = sourceManager.get(sourceId) as HttpSourceBase?

                    mView?.onBindSourceInfo(source!!)

                    if (sourceHttp != null) {
                        onGetPublishers(sourceHttp, sourceId)
                    }
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    override fun onGetPublishers(source: HttpSourceBase, sourceId: Long) {

        mSubscribers.add(homeInterceptor.onGetPublishers()
                .doOnNext {
                    val sourceHttp = sourceManager.get(sourceId) as HttpSourceBase?

                    onGetHomePageData(sourceHttp!!)
                }
                .map { it.map { it.toPublisherModel() } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { publishers ->
                            mView?.onBindPublishers(publishers)
                        },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    override fun onGetHomePageData(source: HttpSourceBase) {

       mSubscribers.add(homeInterceptor.onGetPopularComics()
                .map { it.map { it.toModel() } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ popularComics ->
                    if (popularComics != null) mView!!.onBindPopulars(popularComics)
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))

        mSubscribers.add(homeInterceptor.onGetLastestComics()
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.map { it.toModel() } }
                .subscribe({ lastestsComics ->
                    if (lastestsComics != null) mView!!.onBindLastestUpdates(lastestsComics)
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    override fun onGetMorePublishers() {
        if (homeInterceptor.hasMorePublishers()) {
            mSubscribers.add(homeInterceptor.onGetMorePublishers()
                    .subscribeOn(Schedulers.io())
                    .map { it.map { it.toPublisherModel() } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindMorePublishers(it) },
                            { error ->
                                Timber.e(error)
                                mView?.onError(error)
                            })
                    )
        }
    }

    override fun onGetMorePopularComics() {
        if (homeInterceptor.hasMorePopularComics()) {
            mSubscribers.add(homeInterceptor.onGetMorePopularComics()
                    .subscribeOn(Schedulers.io())
                    .map { it.map { it.toModel() } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindMorePopulars(it) },
                            { error ->
                                Timber.e(error)
                                mView?.onError(error)
                            })
                    )
        }
    }

    override fun onGetMoreLastestComics() {
        if (homeInterceptor.hasMoreLastestComics()) {
            mSubscribers.add(homeInterceptor.onGetMoreLastestComics()
                    .subscribeOn(Schedulers.io())
                    .map { it.map { it.toModel() } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { mView!!.onBindMoreLastestUpdates(it) },
                            { error ->
                                Timber.e(error)
                                mView?.onError(error)
                            })
                    )
        }
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {

        mSubscribers.add(comicRepository.addOrRemoveFromFavorite(comic, comic.source?.id!!)
                .subscribeOn(Schedulers.io())
                .map { it.toModel() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ c ->
                    if (c?.comic?.tags != null) {
                        if (c.comic.tags!!.contains(IComic.POPULARS)) {
                            mView?.onBindPopularItem(c)
                        }
                        if (c.comic.tags!!.contains(IComic.RECENTS)) {
                            mView?.onBindLastestItem(c)
                        }
                    }
                }, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }))
    }

    private fun ComicViewModel.toModel(): ComicItem {
        return ComicItem(this, R.layout.item_comic)
    }

    private fun DefaultModelView.toPublisherModel(): PublisherItem {
        return PublisherItem(this)
    }

}