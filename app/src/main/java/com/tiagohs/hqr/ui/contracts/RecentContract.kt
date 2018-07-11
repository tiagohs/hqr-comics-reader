package com.tiagohs.hqr.ui.contracts

import android.content.Context
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.presenter.config.IPresenter
import com.tiagohs.hqr.ui.views.config.IView

class RecentContract {

    interface IRecentView: IView {
        fun onBindUserHistories(histories: List<ComicDetailsListItem>)
        fun onHistoryRemoved(position: Int)
        fun onBindItem(historyItem: ComicDetailsListItem)
        fun onBindMoreUserHistories(histories: List<ComicDetailsListItem>)
    }

    interface IRecentPresenter: IPresenter<IRecentView> {

        fun onGetUserHistories(context: Context?)
        fun onGetMoreComics()

        fun hasMoreComics(): Boolean
        fun getOriginalList(): List<ComicDetailsListItem>

        fun onRemoveHistory(comicItem: ComicDetailsListItem, position: Int)
        fun addOrRemoveFromFavorite(comicItem: ComicDetailsListItem)
    }
}