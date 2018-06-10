package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class SearchContract {


    interface ISearchView: IView {

        fun onBindComics(comics: List<ComicsItem>?)
    }

    interface ISearchPresenter: IPresenter<ISearchView> {

        fun onSearchComics(query: String)
        fun hasMoreComics(): Boolean
        fun hasPagesSupport(): Boolean
        fun onGetMoreComics()
    }
}