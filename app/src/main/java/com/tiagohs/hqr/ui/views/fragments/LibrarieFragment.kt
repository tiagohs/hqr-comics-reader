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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle(getString(R.string.lirarie_title))
        onCofigureLibrariePageView()
    }
    override fun onErrorAction() {}

    private fun onCofigureLibrariePageView() {
        librarieViewpager.adapter = LibrariePagerAdapter(childFragmentManager, resources.getStringArray(R.array.librarie_tabs_values).toList())
        tabLibrarieLayout.setupWithViewPager(librarieViewpager)
    }

}