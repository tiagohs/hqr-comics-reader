package com.tiagohs.hqr.ui.presenter

import android.content.Context
import android.util.Log
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.interceptors.config.Contracts
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.contracts.FavoritesContract
import com.tiagohs.hqr.ui.presenter.config.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FavoritesPresenter(
        val favoritesInterceptor: Contracts.IFavoritesInterceptor,
        val comicsRepository: IComicsRepository
): BasePresenter<FavoritesContract.IFavoritesView>(), FavoritesContract.IFavoritesPresenter {

    override fun onBindView(view: FavoritesContract.IFavoritesView, context: Context) {
        super.onBindView(view)

        favoritesInterceptor.onBind()
        favoritesInterceptor.onSubscribeInitializa(context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ comicItem -> mView?.onBindItem(comicItem) },
                        { error -> Log.e("FAVORITES", "Inicialização Falhou ", error) })
    }

    override fun onGetFavorites(context: Context) {
        favoritesInterceptor.onGetFavorites(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items -> mView?.onBindComics(items) },
                        { error -> Log.e("FAVORITES", "onGetFavorites Falhou ", error) })
    }

    override fun onGetMoreComics() {
        favoritesInterceptor.onGetMore()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items -> mView?.onBindComics(items) },
                        { error -> Log.e("FAVORITES", "onGetMoreComics Falhou ", error) })
    }

    override fun hasMoreComics(): Boolean {
        return favoritesInterceptor.hasMoreComics()
    }

    override fun getOriginalList(): List<ComicDetailsListItem> {
        return favoritesInterceptor.getOriginalList()
    }

    override fun deleteChapters(comicDetailsListItem: ComicDetailsListItem) {
        comicsRepository.addOrRemoveFromFavorite(comicDetailsListItem.comic, comicDetailsListItem.comic.source?.id!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mView?.onComicRemoved() },
                        { error ->
                            Log.e("Fav", "Error", error)
                            mView?.onComicRemovedError() })
    }


}