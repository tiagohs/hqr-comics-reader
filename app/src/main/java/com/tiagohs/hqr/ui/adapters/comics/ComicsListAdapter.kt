package com.tiagohs.hqr.ui.adapters.comics

import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class ComicsListAdapter(
        var items: List<ComicItem>,
        private val listener: IComicListCallback) : FlexibleAdapter<ComicItem>(items, listener, true) {

    override fun updateDataSet(items: List<ComicItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    override fun clear() {
        this.items = emptyList()

        super.clear()
    }

    fun indexOf(item: ComicItem): Int {
        return items.indexOf(item)
    }

    fun onAddMoreItems(items: List<ComicItem>?) {
        updateDataSet(items)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        listener.addOrRemoveFromFavorite(comic)
    }

}