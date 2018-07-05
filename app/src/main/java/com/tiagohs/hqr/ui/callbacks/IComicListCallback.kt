package com.tiagohs.hqr.ui.callbacks

import com.tiagohs.hqr.models.view_models.ComicViewModel
import eu.davidea.flexibleadapter.FlexibleAdapter

interface IComicListCallback: FlexibleAdapter.OnItemClickListener {
    fun addOrRemoveFromFavorite(comic: ComicViewModel)
}