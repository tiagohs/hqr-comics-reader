package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
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
                    private val sourceRepository: ISourceRepository):
            BasePresenter<HomeContract.IHomeView>(subscriber),
            HomeContract.IHomePresenter {

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

        mSubscribers!!.add(source.fetchPopularComics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { popularComics: List<ComicsItem>? ->
                            if (popularComics!!.isNotEmpty()) mView!!.onBindPopulars(popularComics)
                        },
                        { error: Throwable? ->
                            Log.e("HomePresenter", "Error!", error)
                        }))

        mSubscribers!!.add(source.fetchLastestComics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { lastestComics: List<ComicsItem>? ->
                            if (lastestComics!!.isNotEmpty()) mView!!.onBindLastestUpdates(lastestComics)
                        },
                        { error: Throwable? ->
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