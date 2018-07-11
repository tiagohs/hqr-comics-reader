package com.tiagohs.hqr.ui.callbacks

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import eu.davidea.flexibleadapter.FlexibleAdapter

interface IComicDetailsListCallback: FlexibleAdapter.OnItemClickListener {

    fun onMenuClick(position: Int, menuItem: MenuItem)
    fun onMenuCreate(inflater: MenuInflater, menu: Menu)
    fun onPrepareMenu(menu: Menu, position: Int, item: ComicDetailsListItem)
}