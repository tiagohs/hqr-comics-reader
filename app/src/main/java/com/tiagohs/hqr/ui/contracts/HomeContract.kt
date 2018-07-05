package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class HomeContract {

    interface IHomeView: IView {

        fun onBindPublishers(publishers: List<Publisher>)
        fun onBindPopulars(populars: List<ComicItem>)
        fun onBindLastestUpdates(lastestUpdates: List<ComicItem>)
        fun onBindSourceInfo(source: ISource)

        fun onBindPopularItem(comic: ComicItem)
        fun onBindLastestItem(comic: ComicItem)
    }

    interface IHomePresenter: IPresenter<IHomeView> {

        fun onGetHomeData(sourceId: Long)
        fun observeSourcesChanges()
        fun onGetPublishers(source: HttpSourceBase)
        fun onGetHomePageData(source: HttpSourceBase)

        fun addOrRemoveFromFavorite(comic: ComicViewModel)
    }
}