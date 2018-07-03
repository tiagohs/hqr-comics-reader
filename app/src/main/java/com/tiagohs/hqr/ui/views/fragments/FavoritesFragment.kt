package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.contracts.FavoritesContract
import com.tiagohs.hqr.ui.views.config.BaseFragment

class FavoritesFragment: BaseFragment(), FavoritesContract.IFavoritesView {

    companion object {
        fun newFragment(): FavoritesFragment = FavoritesFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_favorites

}