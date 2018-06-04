package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.sources.ChapterItem

interface IChapterItemCallback {

    fun onChapterSelect(chapter: ChapterItem)
    fun onDownloadSelect(chapter: ChapterItem)
}