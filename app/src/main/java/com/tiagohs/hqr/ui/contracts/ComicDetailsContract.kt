package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ComicDetailsContract {

    interface IComicDetailsView: IView {

        fun onBindComic(comic: Comic)
    }

    interface IComicDetailsPresenter: IPresenter<IComicDetailsView> {

        fun onGetComicData(comicPath: String)
    }
}