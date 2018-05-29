package com.tiagohs.hqr.ui.presenter

import android.util.Log
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.sources.portuguese.HQBRSource
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class HomePresenter(subscriber: CompositeDisposable,
                    private val source: HQBRSource):
            BasePresenter<HomeContract.IHomeView>(subscriber),
            HomeContract.IHomePresenter {

    override fun onGetPublishers() {
        source.fetchPublishers()
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
                })
    }

    override fun onGetHomePageData() {

        source.fetchPopularComics()
              .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { popularComics: List<ComicsItem>? ->
                        if (popularComics!!.isNotEmpty()) mView!!.onBindPopulars(popularComics)
                    },
                    { error: Throwable? ->
                        Log.e("HomePresenter", "Error!", error)
               })

        source.fetchLastestComics()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { lastestComics: List<ComicsItem>? ->
                        if (lastestComics!!.isNotEmpty()) mView!!.onBindLastestUpdates(lastestComics)
                    },
                    { error: Throwable? ->
                        Log.e("HomePresenter", "Error!", error)
                })
    }

}