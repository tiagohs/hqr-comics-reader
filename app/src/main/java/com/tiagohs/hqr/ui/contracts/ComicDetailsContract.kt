package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class ComicDetailsContract {

    interface IComicDetailsView: IView {

        fun onBindComic(comic: ComicViewModel)
        fun onConfigureFavoriteBtn(comic: ComicViewModel)
    }

    interface IComicDetailsPresenter: IPresenter<IComicDetailsView> {

        fun onGetComicData(comicPath: String)
        fun addOrRemoveFromFavorite(comic: ComicViewModel)
    }
}