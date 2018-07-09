package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.adapters.publishers.PublisherItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class HomeContract {

    interface IHomeView: IView {

        fun onBindSourceInfo(source: ISource)

        fun onBindPublishers(publishers: List<PublisherItem>)
        fun onBindPopulars(populars: List<ComicItem>)
        fun onBindLastestUpdates(lastestUpdates: List<ComicItem>)

        fun onBindMorePublishers(publishers: List<PublisherItem>)
        fun onBindMorePopulars(populars: List<ComicItem>)
        fun onBindMoreLastestUpdates(lastestUpdates: List<ComicItem>)

        fun onBindPopularItem(comic: ComicItem)
        fun onBindLastestItem(comic: ComicItem)
    }

    interface IHomePresenter: IPresenter<IHomeView> {

        fun onGetHomeData(sourceId: Long)
        fun observeSourcesChanges()

        fun onGetPublishers(source: HttpSourceBase, sourceId: Long)
        fun onGetHomePageData(source: HttpSourceBase)

        fun onGetMorePublishers()
        fun onGetMorePopularComics()
        fun onGetMoreLastestComics()

        fun addOrRemoveFromFavorite(comic: ComicViewModel)
    }
}