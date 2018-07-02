package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.ui.adapters.chapters.ChapterItem
import eu.davidea.flexibleadapter.FlexibleAdapter

interface IChapterItemCallback: FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    fun onDownloadSelect(chapter: ChapterItem)
}