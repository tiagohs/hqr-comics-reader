package com.tiagohs.hqr.ui.presenter

import android.content.Context
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.contracts.RecentContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class RecentPresenter(
        private val preferenceHelper: PreferenceHelper,
        private val comicRepository: IComicsRepository,
        private val historyRepository: IHistoryRepository,
        private val localeUtils: LocaleUtils
): BasePresenter<RecentContract.IRecentView>(), RecentContract.IRecentPresenter {

    var listPaginator: ListPaginator<ComicDetailsListItem> = ListPaginator()

    override fun onGetUserHistories(context: Context?) {

        mSubscribers.add(historyRepository.findAll()
                .map {
                    val comicHistoryItems = it.map { it.toModel(context, it.comic) }

                    listPaginator.onCreatePagination(comicHistoryItems)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ histories -> mView?.onBindUserHistories(histories) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    override fun onGetMoreComics() {
        mSubscribers.add(listPaginator.onGetNextPage()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindMoreUserHistories(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    override fun hasMoreComics(): Boolean {
        return listPaginator.hasMorePages
    }

    override fun getOriginalList(): List<ComicDetailsListItem> {
        return listPaginator.originalList
    }

    override fun onRemoveHistory(comicItem: ComicDetailsListItem, position: Int) {
        mSubscribers.add(historyRepository.deleteComicHistory(comicItem.history!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { error ->
                    Timber.e(error)
                    mView?.onError(error)
                }, {
                    mView?.onHistoryRemoved(position)
                }))
    }

    override fun addOrRemoveFromFavorite(comicItem: ComicDetailsListItem) {
        val sourceId = preferenceHelper.currentSource().getOrDefault()

        mSubscribers.add(comicRepository.addOrRemoveFromFavorite(comicItem.comic, sourceId)
                .subscribeOn(Schedulers.io())
                .map {
                    comicItem.comic.favorite = it.favorite
                    comicItem
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onBindItem(it) },
                        { error ->
                            Timber.e(error)
                            mView?.onError(error)
                        }))
    }

    private fun ComicHistoryViewModel.toModel(context: Context?, comic: ComicViewModel?): ComicDetailsListItem {
        return ComicDetailsListItem(comic!!, localeUtils, context!!, this)
    }
}