package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.view_models.ComicViewModel

interface IComicListCallback {

    fun onComicSelect(comic: ComicViewModel)
}