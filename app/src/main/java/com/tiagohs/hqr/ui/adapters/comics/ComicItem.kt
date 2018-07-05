package com.tiagohs.hqr.ui.adapters.comics

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.models.view_models.ComicViewModel
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class ComicItem(
        val comic: ComicViewModel,
        val layoutId: Int
): AbstractFlexibleItem<ComicHolder>() {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ComicHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ComicItem) {
            return comic.id.equals(other.comic.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return comic.id.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ComicHolder {
        return ComicHolder(view, adapter as ComicsListAdapter)
    }

    override fun getLayoutRes(): Int = layoutId

}