package com.tiagohs.hqr.ui.adapters.chapters

import android.content.Context
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.callbacks.IChapterItemCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class ChaptersListAdapter(
        context: Context?,
        private val listener: IChapterItemCallback) : FlexibleAdapter<ChapterItem>(null, listener, true) {

    var items: List<ChapterItem> = emptyList()

    override fun updateDataSet(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun onDownloadButtonClicked(chapterItem: ChapterItem, comicViewModel: ComicViewModel) {
        listener.onDownloadSelect(chapterItem)
    }

}