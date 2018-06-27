package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.viewModels.ComicViewModel

interface IComicListCallback {

    fun onComicSelect(comic: ComicViewModel)
}