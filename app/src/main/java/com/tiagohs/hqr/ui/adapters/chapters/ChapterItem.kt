package com.tiagohs.hqr.ui.adapters.chapters

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class ChapterItem(
        val chapter: ChapterViewModel,
        val comic: ComicViewModel
): AbstractFlexibleItem<ChapterHolder>() {

    private var _status: String = "NOT_DOWNLOADED"

    var status: String
        get() = download?.status ?: _status
        set(value) { _status = value }

    @Transient var download: Download? = null

    val isDownloaded: Boolean
        get() = status == Download.DOWNLOADED

    override fun getLayoutRes(): Int = R.layout.item_chapter

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ChapterHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(this, comic)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ChapterItem) {
            return chapter.id.equals(other.chapter.id)
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ChapterHolder {
        return ChapterHolder(view, adapter as ChaptersListAdapter)
    }

}