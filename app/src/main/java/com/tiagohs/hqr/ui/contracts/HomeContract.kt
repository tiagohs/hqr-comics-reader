package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class HomeContract {

    interface IHomeView: IView {

        fun onBindPublishers(publishers: List<Publisher>)
        fun onBindPopulars(populars: List<ComicViewModel>)
        fun onBindLastestUpdates(lastestUpdates: List<ComicViewModel>)
        fun onBindSourceInfo(source: ISource)

        fun onBindPopularItem(comic: ComicViewModel)
        fun onBindLastestItem(comic: ComicViewModel)
    }

    interface IHomePresenter: IPresenter<IHomeView> {

        fun onGetHomeData(sourceId: Long)
        fun observeSourcesChanges()
        fun onGetPublishers(source: HttpSourceBase)
        fun onGetHomePageData(source: HttpSourceBase)

    }
}