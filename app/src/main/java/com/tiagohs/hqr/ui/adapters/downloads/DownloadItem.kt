package com.tiagohs.hqr.ui.adapters.downloads

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class DownloadItem(
        val comic: ComicViewModel,
        val localeUtils: LocaleUtils,
        val context: Context
): AbstractFlexibleItem<DownloadHolder>() {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: DownloadHolder, position: Int, payloads: MutableList<Any>?) {
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

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): DownloadHolder {
        return DownloadHolder(view, adapter as DownloadsAdapter)
    }

    override fun getLayoutRes(): Int = R.layout.item_downloads
}