package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.sources.ComicsItem

interface IComicListCallback {

    fun onComicSelect(comic: ComicsItem)
}