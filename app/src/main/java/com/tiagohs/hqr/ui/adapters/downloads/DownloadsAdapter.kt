package com.tiagohs.hqr.ui.adapters.downloads

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.tiagohs.hqr.ui.callbacks.IDownloadCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class DownloadsAdapter(
    private val listener: IDownloadCallback
): FlexibleAdapter<DownloadItem>(null, listener, true) {

    var items: List<DownloadItem> = emptyList()

    override fun updateDataSet(items: List<DownloadItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun indexOf(item: DownloadItem): Int {
        return items.indexOf(item)
    }

    fun onAddMoreItems(items: List<DownloadItem>?) {
        updateDataSet(items)

        notifyDataSetChanged()
    }

    fun onMenuClick(position: Int, menuItem: MenuItem) {
        listener.onMenuClick(position, menuItem)
    }

    fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        listener.onMenuCreate(inflater, menu)
    }

    fun onPrepareMenu(menu: Menu, position: Int, item: DownloadItem) {
        listener.onPrepareMenu(menu, position, item)
    }

    fun addOrRemoveFromFavorite(downloadItem: DownloadItem) {
        listener.addOrRemoveFromFavorite(downloadItem)
    }

}