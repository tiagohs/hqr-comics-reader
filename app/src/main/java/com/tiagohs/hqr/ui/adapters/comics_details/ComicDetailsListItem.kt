package com.tiagohs.hqr.ui.adapters.comics_details

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class ComicDetailsListItem(
        val comic: ComicViewModel,
        val localeUtils: LocaleUtils,
        val context: Context,
        val history: ComicHistoryViewModel? = null
): AbstractFlexibleItem<ComicDetailsListHolder>() {

    fun getSourceName(): String {
        return comic.source!!.name!!
    }

    fun getLanguage(): String {
        return comic.source!!.language.toUpperCase()
    }

    fun getLastChapterReaded(): ChapterViewModel? {
        return history?.chapter
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ComicDetailsListHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ComicDetailsListItem) {
            return comic.id == other.comic.id
        }
        return false
    }

    override fun hashCode(): Int {
        return comic.id.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ComicDetailsListHolder {
        return ComicDetailsListHolder(view, adapter as ComicDetailsListAdapter)
    }

    override fun getLayoutRes(): Int = R.layout.item_comic_detail
}