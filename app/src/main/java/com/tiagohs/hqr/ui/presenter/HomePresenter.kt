package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class HomePresenter(subscriber: CompositeDisposable,
                    private val sourceManager: SourceManager,
                    private val preferenceHelper: PreferenceHelper,
                    private val sourceRepository: ISourceRepository,
                    private val comicsInterceptor: Contracts.IComicsInterceptor):
            BasePresenter<HomeContract.IHomeView>(subscriber),
            HomeContract.IHomePresenter {

    override fun onBindView(view: HomeContract.IHomeView) {
        super.onBindView(view)

        comicsInterceptor.onBind()
        comicsInterceptor.subscribeComicDetailSubject()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe({ comic: ComicViewModel? ->
                             Log.d("HOME", "Inicialização: " + comic?.name)

                             if (comic?.tags != null) {
                                 if (comic.tags!!.contains(IComic.POPULARS)) {
                                     mView?.onBindPopularItem(comic)
                                 }
                                 if (comic.tags!!.contains(IComic.RECENTS)) {
                                     mView?.onBindLastestItem(comic)
                                 }
                             }

                         }, { error ->
                             Log.e("Home", "Inicialização Falhou ", error)
                         })
    }

    override fun onUnbindView() {
        super.onUnbindView()

        comicsInterceptor.onUnbind()
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

        mSubscribers!!.add(source.fetchPublishers()
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

        mSubscribers!!.add(comicsInterceptor.onGetPopularComics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ popularComics: List<ComicViewModel>? ->
                    if (popularComics != null) mView!!.onBindPopulars(popularComics)
                }, { error ->
                    Log.e("HomePresenter", "Error!", error)
                }))

        mSubscribers!!.add(comicsInterceptor.onGetLastestComics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ lastestsComics: List<ComicViewModel>? ->
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

}