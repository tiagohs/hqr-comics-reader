package com.tiagohs.hqr.ui.adapters.comics_details

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.tiagohs.hqr.ui.callbacks.IFavoritesCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class ComicDetailsListAdapter(
        val showHistory: Boolean = false,
        val listener: IFavoritesCallback) : FlexibleAdapter<ComicDetailsListItem>(null, listener, true) {

    var items: List<ComicDetailsListItem> = emptyList()

    override fun updateDataSet(items: List<ComicDetailsListItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun onMenuClick(position: Int, menuItem: MenuItem) {
        listener.onMenuClick(position, menuItem)
    }

    fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        listener.onMenuCreate(inflater, menu)
    }

    fun onPrepareMenu(menu: Menu, position: Int, item: ComicDetailsListItem) {
        listener.onPrepareMenu(menu, position, item)
    }
}