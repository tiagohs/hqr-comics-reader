package com.tiagohs.hqr.interceptors

import android.content.Context
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.IHistoryRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.ListPaginator
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.interceptors.config.BaseComicsInterceptor
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.SourceManager
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import io.reactivex.Observable

class FavoritesInterceptor(
        private val preferenceHelper: PreferenceHelper,
        private val comicsRepository: IComicsRepository,
        private val historyRepository: IHistoryRepository,
        private val sourceRepository: ISourceRepository,
        private val sourceManager: SourceManager,
        private val localeUtils: LocaleUtils
): BaseComicsInterceptor(comicsRepository, preferenceHelper, sourceManager, sourceRepository), Contracts.IFavoritesInterceptor {

    var listPaginator: ListPaginator<ComicDetailsListItem> = ListPaginator()

    override fun onSubscribeInitializa(context: Context): Observable<ComicDetailsListItem> {
        return subscribeComicDetailSubject()
                .map { it.toModel(context)  }
    }

    override fun onGetFavorites(context: Context): Observable<List<ComicDetailsListItem>> {
        return comicsRepository.getFavoritesComics()
                .doOnNext { comics -> initializeComics(comics) }
                .map {
                    val comicHistoryItems = it.map { it.toModel(context) }

                    listPaginator.onCreatePagination(comicHistoryItems)
                }
    }

    override fun onGetMore(): Observable<List<ComicDetailsListItem>> {
        return listPaginator.onGetNextPage()
                              .doOnNext { comicHistoryItems -> initializeComics(comicHistoryItems.map { it.comic }) }
    }

    override fun hasMoreComics(): Boolean {
        return listPaginator.hasMorePages
    }

    override fun getOriginalList(): List<ComicDetailsListItem> {
        return listPaginator.originalList
    }

    private fun ComicViewModel.toModel(context: Context): ComicDetailsListItem {
        return ComicDetailsListItem(this, localeUtils, context, historyRepository.findByComicIdRealm(id))
    }
}