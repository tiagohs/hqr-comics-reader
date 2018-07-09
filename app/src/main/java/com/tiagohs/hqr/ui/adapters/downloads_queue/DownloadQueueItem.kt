package com.tiagohs.hqr.ui.adapters.downloads_queue

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.Download
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class DownloadQueueItem(
        val download: Download
): AbstractFlexibleItem<DownloadQueueHolder>() {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: DownloadQueueHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(download)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Download) {
            return download.chapter.id == other.chapter.id
        }
        return false
    }

    override fun hashCode(): Int {
        return download.chapter.id.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): DownloadQueueHolder {
        return DownloadQueueHolder(view, adapter as DownloadQueueAdapter)
    }

    override fun getLayoutRes(): Int = R.layout.item_download_manager_list
}