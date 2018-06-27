package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ListComicsContract {

    interface IListComicsView: IView {

        fun onBindComics(comics: List<ComicViewModel>?)
        fun onBindMoreComics(comics: List<ComicViewModel>)
    }

    interface IListComicsPresenter: IPresenter<IListComicsView> {

        fun onGetComics(listType: String, flag: String)
        fun hasMoreComics(): Boolean
        fun hasPagesSupport(): Boolean
        fun getOriginalList(): List<ComicViewModel>
        fun onGetMoreComics()
    }
}