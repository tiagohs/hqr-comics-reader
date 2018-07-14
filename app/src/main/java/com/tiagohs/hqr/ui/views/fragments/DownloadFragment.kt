package com.tiagohs.hqr.ui.views.fragments

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.adapters.comics_details.ComicDetailsListItem
import com.tiagohs.hqr.ui.adapters.downloads.DownloadItem
import com.tiagohs.hqr.ui.callbacks.IDownloadCallback
import com.tiagohs.hqr.ui.contracts.DownloadContract
import com.tiagohs.hqr.ui.views.config.BaseFragment

class DownloadFragment:
        BaseFragment(),
        DownloadContract.IDownloadView,
        IDownloadCallback {

    companion object {
        fun newFragment(): DownloadFragment = DownloadFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_downloads

    override fun onMenuClick(position: Int, menuItem: MenuItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onMenuCreate(inflater: MenuInflater, menu: Menu) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPrepareMenu(menu: Menu, position: Int, item: ComicDetailsListItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindItem(downloadItem: DownloadItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindDownloads(downloads: List<DownloadItem>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindMoreDownloads(downloads: List<DownloadItem>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}