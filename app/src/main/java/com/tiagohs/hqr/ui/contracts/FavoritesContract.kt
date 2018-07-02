package com.tiagohs.hqr.ui.contracts

import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class FavoritesContract {

    interface IFavoritesView: IView {

    }

    interface IFavoritesPresenter: IPresenter<IFavoritesView> {

    }
}