package com.tiagohs.hqr.models.view_models

import com.tiagohs.hqr.models.sources.Page

class ReaderChapterViewModel(
        val chapter: ChapterViewModel,
        val comic: ComicViewModel
) {

    var pages: List<Page> = emptyList()

    var isDownloaded: Boolean = false
}