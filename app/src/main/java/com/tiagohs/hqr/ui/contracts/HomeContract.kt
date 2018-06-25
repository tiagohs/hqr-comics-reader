package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class HomeContract {

    interface IHomeView: IView {

        fun onBindPublishers(publishers: List<Publisher>)
        fun onBindPopulars(populars: List<ComicsItem>)
        fun onBindLastestUpdates(lastestUpdates: List<ComicsItem>)
        fun onBindSourceInfo(source: ISource)
    }

    interface IHomePresenter: IPresenter<IHomeView> {

        fun onGetHomeData(sourceId: Long)
        fun observeSourcesChanges()
        fun onGetPublishers(source: HttpSourceBase)
        fun onGetHomePageData(source: HttpSourceBase)
    }
}