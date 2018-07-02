package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.contracts.RecentContract
import com.tiagohs.hqr.ui.views.config.BaseFragment

class RecentFragment: BaseFragment(), RecentContract.IRecentView {

    companion object {
        fun newFragment(): RecentFragment = RecentFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_recent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle("Recentemente lidos")
    }

}