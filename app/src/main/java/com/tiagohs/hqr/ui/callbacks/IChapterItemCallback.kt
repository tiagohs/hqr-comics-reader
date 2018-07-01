package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.view_models.ChapterViewModel

interface IChapterItemCallback {

    fun onChapterSelect(chapter: ChapterViewModel)
    fun onDownloadSelect(chapter: ChapterViewModel)
}