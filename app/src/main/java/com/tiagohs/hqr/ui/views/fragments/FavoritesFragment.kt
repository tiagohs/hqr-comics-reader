package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.tools.EndlessRecyclerView
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListAdapter
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListHolder
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.callbacks.IComicDetailsListCallback
import com.tiagohs.hqr.ui.contracts.FavoritesContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_favorites.*
import javax.inject.Inject

class FavoritesFragment: BaseFragment(), FavoritesContract.IFavoritesView, IComicDetailsListCallback {

    companion object {
        fun newFragment(): FavoritesFragment = FavoritesFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_favorites

    @Inject
    lateinit var presenter: FavoritesContract.IFavoritesPresenter

    var adapter: ComicDetailsListAdapter? = null
    var layoutManager: RecyclerView.LayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()?.inject(this)

        presenter.onBindView(this, context!!)
        presenter.onGetFavorites(context!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.onUnbindView()
    }

    override fun onError(ex: Throwable, message: Int) {
        favoritesListProgress.visibility = View.GONE

        setInformationViewStatus()

        super.onError(ex, message)
    }

    override fun onErrorAction() {
        presenter.onGetFavorites(context!!)

        dismissSnack()
    }

    override fun onBindComics(comics: List<ComicDetailsListItem>?) {
        adapter = ComicDetailsListAdapter(true, this)
        adapter?.updateDataSet(comics)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        favoritesList.adapter = adapter
        favoritesList.layoutManager = layoutManager
        favoritesList.addOnScrollListener(createOnScrollListener())
        favoritesList.setNestedScrollingEnabled(false)

        favoritesListProgress.visibility = View.GONE

        setInformationViewStatus()
    }

    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(layoutManager!!) {

            override fun onLoadMore(current_page: Int) {
                if (presenter.hasMoreComics()) {
                    presenter.onGetMoreComics()
                }
            }
        }
    }

    override fun onBindMoreComics(comics: List<ComicDetailsListItem>?) {
        adapter?.onAddMoreItems(comics)

        setInformationViewStatus()
    }

    override fun onBindItem(comic: ComicDetailsListItem) {
        val position = adapter?.indexOf(comic) ?: return

        adapter?.updateItem(position, comic, null)
        adapter?.notifyItemChanged(position)
    }

    private fun getHolder(comic: ComicDetailsListItem): ComicDetailsListHolder? {
        return favoritesList?.findViewHolderForItemId(comic.comic.id) as? ComicDetailsListHolder
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val comic = adapter?.getItem(position) ?: return false
        startActivity(ComicDetailsActivity.newIntent(context, comic.comic.pathLink!!))

        return true
    }

    override fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.menu_comics_details_list, menu)
    }

    override fun onPrepareMenu(menu: Menu, position: Int, item: ComicDetailsListItem) {}

    override fun onMenuClick(position: Int, menuItem: MenuItem) {
        val item = adapter?.getItem(position) ?: return

        when(menuItem.itemId) {
            R.id.actionDelete -> deleteComic(item, position)
        }
    }

    private fun deleteComic(item: ComicDetailsListItem, position: Int) {
        adapter?.removeItem(position)
        presenter.deleteChapters(item, position)
    }

    override fun onComicRemoved(position: Int) {
        setInformationViewStatus()
    }

    override fun onComicRemovedError() {

    }

    fun setInformationViewStatus() {
        val adapter = adapter ?: return

        if (adapter.isEmpty) {
            favoriteEmptyView.show(R.drawable.ic_check_circle_black_128dp, R.string.no_favorites)
        } else {
            favoriteEmptyView.hide()
        }

    }

}