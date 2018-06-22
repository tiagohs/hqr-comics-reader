package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.sources.Chapter

interface IChapterItemCallback {

    fun onChapterSelect(chapter: Chapter)
    fun onDownloadSelect(chapter: Chapter)
}