package com.tiagohs.hqr.ui.adapters.comics

import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class ComicsListAdapter(
        private val listener: IComicListCallback) : FlexibleAdapter<ComicItem>(null, listener, true) {

    var items: List<ComicItem> = emptyList()

    override fun updateDataSet(items: List<ComicItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ComicItem): Int {
        return items.indexOf(item)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        listener.addOrRemoveFromFavorite(comic)
    }

}