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

    override fun clear() {
        this.items = emptyList()

        super.clear()
    }

    fun onAddMoreItems(items: List<ComicItem>?) {
        updateDataSet(items)

        notifyDataSetChanged()
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        listener.addOrRemoveFromFavorite(comic)
    }

}