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
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.callbacks.IComicDetailsListCallback
import com.tiagohs.hqr.ui.contracts.RecentContract
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_recent.*
import javax.inject.Inject

class RecentFragment: BaseFragment(), RecentContract.IRecentView, IComicDetailsListCallback {

    companion object {
        fun newFragment(): RecentFragment = RecentFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_recent

    @Inject
    lateinit var presenter: RecentContract.IRecentPresenter

    var adapter: ComicDetailsListAdapter? = null
    var layoutManager: RecyclerView.LayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle("Recentemente lidos")

        getApplicationComponent()?.inject(this)

        presenter.onBindView(this)
        presenter.onGetUserHistories(context)
    }

    override fun onBindUserHistories(histories: List<ComicDetailsListItem>) {
        adapter = ComicDetailsListAdapter(true, this)
        adapter?.updateDataSet(histories)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        recentList.adapter = adapter
        recentList.layoutManager = layoutManager
        recentList.addOnScrollListener(createOnScrollListener())
        recentList.setNestedScrollingEnabled(false)

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

    override fun onBindMoreUserHistories(histories: List<ComicDetailsListItem>) {
        adapter?.onAddMoreItems(histories)

        setInformationViewStatus()
    }

    override fun onHistoryRemoved(position: Int) {
        adapter?.notifyItemRemoved(position)
    }

    override fun onBindItem(historyItem: ComicDetailsListItem) {
        val position = adapter?.indexOf(historyItem) ?: return

        adapter?.updateItem(position, historyItem, null)
        adapter?.notifyItemChanged(position)
    }

    override fun onMenuClick(position: Int, menuItem: MenuItem) {
        val item = adapter?.getItem(position) ?: return

        when(menuItem.itemId) {
            R.id.actionDelete -> deleteComic(item, position)
        }
    }

    private fun deleteComic(item: ComicDetailsListItem, position: Int) {
        adapter?.removeItem(position)
        presenter.onRemoveHistory(item, position)
    }

    override fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.menu_comics_details_list, menu)
    }

    override fun onPrepareMenu(menu: Menu, position: Int, item: ComicDetailsListItem) {}

    override fun onItemClick(view: View?, position: Int): Boolean {
        val historyItem = adapter?.getItem(position) ?: return false
        startActivity(ReaderActivity.newIntent(context, historyItem.getLastChapterReaded()?.chapterPath!!, historyItem.comic.pathLink!!))

        return true
    }

    fun setInformationViewStatus() {
        val adapter = adapter ?: return

        if (adapter.isEmpty) {
            recentEmptyView.show(R.drawable.ic_person_read_grey_128, R.string.no_recent)
        } else {
            recentEmptyView.hide()
        }

    }

}