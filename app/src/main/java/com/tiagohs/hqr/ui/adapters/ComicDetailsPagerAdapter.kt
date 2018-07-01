package com.tiagohs.hqr.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.views.fragments.ComicChaptersFragment
import com.tiagohs.hqr.ui.views.fragments.ComicResumeFragment

class ComicDetailsPagerAdapter(fm: FragmentManager,
                               private var tabsName: List<String>,
                               private var comic: ComicViewModel) : FragmentStatePagerAdapter(fm) {

    private val TAB_DETAILS = 0
    private val TAB_CHAPTERS = 1

    override fun getCount(): Int  = tabsName.size

    override fun getItem(tabSelect: Int): Fragment {

        when (tabSelect) {
            TAB_DETAILS -> return ComicResumeFragment.newFragment(comic)
            TAB_CHAPTERS -> return ComicChaptersFragment.newFragment(comic)
            else -> return ComicResumeFragment.newFragment(comic)
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabsName.get(position)
    }
}
