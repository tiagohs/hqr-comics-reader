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
import com.tiagohs.hqr.ui.adapters.downloads.DownloadItem
import com.tiagohs.hqr.ui.adapters.downloads.DownloadsAdapter
import com.tiagohs.hqr.ui.callbacks.IDownloadCallback
import com.tiagohs.hqr.ui.contracts.DownloadContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_downloads.*
import javax.inject.Inject

class DownloadFragment:
        BaseFragment(),
        DownloadContract.IDownloadView,
        IDownloadCallback {

    companion object {
        fun newFragment(): DownloadFragment = DownloadFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_downloads

    @Inject
    lateinit var presenter: DownloadContract.IDownloadPresenter

    private var adapter: DownloadsAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()?.inject(this)

        presenter.onBindView(this)
        presenter.onGetDownloads()
    }

    override fun onError(ex: Throwable, message: Int) {
        downloadsListProgress.visibility = View.GONE
        setInformationViewStatus()

        super.onError(ex, message)
    }

    override fun onErrorAction() {
        downloadsListProgress.visibility = View.VISIBLE

        presenter.onGetDownloads()

        dismissSnack()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.onUnbindView()
    }

    override fun onBindDownloads(downloads: List<DownloadItem>) {
        adapter = DownloadsAdapter(this)
        adapter?.updateDataSet(downloads)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        downloadsList.adapter = adapter
        downloadsList.layoutManager = layoutManager
        downloadsList.addOnScrollListener(createOnScrollListener())
        downloadsList.setNestedScrollingEnabled(false)

        downloadsListProgress.visibility = View.GONE

        setInformationViewStatus()
    }

    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(layoutManager!!) {

            override fun onLoadMore(current_page: Int) {
                if (presenter.hasMore()) {
                    presenter.onGetMore()
                }
            }
        }
    }

    override fun onBindMoreDownloads(downloads: List<DownloadItem>) {
        adapter?.onAddMoreItems(downloads)

        setInformationViewStatus()
    }

    fun setInformationViewStatus() {
        val adapter = adapter ?: return

        if (adapter.isEmpty) {
            downloadsEmptyView.show(R.drawable.ic_file_download_grey_128dp, R.string.no_downloads)
        } else {
            downloadsEmptyView.hide()
        }

    }

    override fun onBindItem(downloadItem: DownloadItem) {
        val position = adapter?.indexOf(downloadItem) ?: return

        adapter?.updateItem(position, downloadItem, null)
        adapter?.notifyItemChanged(position)
    }

    override fun addOrRemoveFromFavorite(downloadItem: DownloadItem) {
        presenter.addOrRemoveFromFavorite(downloadItem)
    }

    override fun onMenuClick(position: Int, menuItem: MenuItem) {
        val item = adapter?.getItem(position) ?: return

        when(menuItem.itemId) {
            R.id.actionDelete -> deleteComic(item, position)
        }
    }

    private fun deleteComic(item: DownloadItem, position: Int) {
        adapter?.removeItem(position)
        presenter.deleteComic(item)

        setInformationViewStatus()
    }

    override fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.menu_comics_details_list, menu)
    }

    override fun onPrepareMenu(menu: Menu, position: Int, item: DownloadItem) {}

    override fun onItemClick(view: View?, position: Int): Boolean {
        val downloadItem = adapter?.getItem(position) ?: return false
        startActivity(ComicDetailsActivity.newIntent(context, downloadItem.comic.pathLink!!))

        return true
    }


}