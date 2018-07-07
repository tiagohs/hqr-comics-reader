package com.tiagohs.hqr.ui.contracts

import android.content.Context
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class FavoritesContract {

    interface IFavoritesView: IView {

        fun onBindComics(comics: List<ComicDetailsListItem>?)
        fun onBindMoreComics(comics: List<ComicDetailsListItem>?)

        fun onBindItem(comic: ComicDetailsListItem)
        fun onComicRemoved()
        fun onComicRemovedError()
    }

    interface IFavoritesPresenter: IPresenter<IFavoritesView> {

        fun onBindView(view: FavoritesContract.IFavoritesView, context: Context)

        fun onGetFavorites(context: Context)

        fun hasMoreComics(): Boolean
        fun onGetMoreComics()
        fun getOriginalList(): List<ComicDetailsListItem>
        fun deleteChapters(comicDetailsListItem: ComicDetailsListItem)
    }
}