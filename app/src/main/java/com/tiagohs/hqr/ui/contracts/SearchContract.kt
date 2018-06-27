package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class SearchContract {


    interface ISearchView: IView {

        fun onBindComics(comics: List<ComicViewModel>?)
    }

    interface ISearchPresenter: IPresenter<ISearchView> {

        fun onSearchComics(query: String)
        fun hasMoreComics(): Boolean
        fun hasPagesSupport(): Boolean
        fun onGetMoreComics()
    }
}