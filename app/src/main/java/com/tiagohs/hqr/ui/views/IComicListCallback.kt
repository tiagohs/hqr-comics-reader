package com.tiagohs.hqr.ui.views

import com.tiagohs.hqr.models.sources.ComicsItem

interface IComicListCallback {

    fun onComicSelect(comic: ComicsItem)
}