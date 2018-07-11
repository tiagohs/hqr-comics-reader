package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.adapters.pagers.LibrariePagerAdapter
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_librarie.*

class LibrarieFragment: BaseFragment() {

    companion object {
        fun newFragment(): LibrarieFragment = LibrarieFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_librarie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle("Minha Biblioteca")
        onCofigureLibrariePageView()
    }

    private fun onCofigureLibrariePageView() {
        librarieViewpager.adapter = LibrariePagerAdapter(childFragmentManager, mutableListOf("Favoritos", "Downloads"))
        tabLibrarieLayout.setupWithViewPager(librarieViewpager)
    }

}