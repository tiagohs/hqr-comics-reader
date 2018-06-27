package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.viewModels.ChapterViewModel

interface IChapterItemCallback {

    fun onChapterSelect(chapter: ChapterViewModel)
    fun onDownloadSelect(chapter: ChapterViewModel)
}