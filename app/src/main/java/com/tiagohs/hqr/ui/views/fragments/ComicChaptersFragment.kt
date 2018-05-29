package com.tiagohs.hqr.ui.views.fragments

import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.views.config.BaseFragment

class ComicChaptersFragment: BaseFragment() {

    companion object {
        fun newFragment(): ComicChaptersFragment {
            return ComicChaptersFragment()
        }
    }

    override fun getViewID(): Int {
        return R.layout.fragment_comic_chapters
    }
}