package com.tiagohs.hqr.ui.views.fragments

import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.contracts.DownloadContract
import com.tiagohs.hqr.ui.views.config.BaseFragment

class DownloadFragment: BaseFragment(), DownloadContract.IDownloadView {

    companion object {
        fun newFragment(): DownloadFragment = DownloadFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_downloads

}