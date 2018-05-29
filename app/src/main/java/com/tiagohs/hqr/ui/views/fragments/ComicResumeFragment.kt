package com.tiagohs.hqr.ui.views.fragments

import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.views.config.BaseFragment

class ComicResumeFragment: BaseFragment() {

    companion object {
        fun newFragment(): ComicResumeFragment {
            return ComicResumeFragment()
        }
    }

    override fun getViewID(): Int {
        return R.layout.fragment_comic_resume
    }
}