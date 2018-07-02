package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.contracts.DownloadManagerContract
import com.tiagohs.hqr.ui.views.config.BaseFragment

class DownloadManagerFragment: BaseFragment(), DownloadManagerContract.IDownloadManagerView {

    companion object {
        fun newFragment(): DownloadManagerFragment = DownloadManagerFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_download_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle("Fila de Downloads")
    }


}