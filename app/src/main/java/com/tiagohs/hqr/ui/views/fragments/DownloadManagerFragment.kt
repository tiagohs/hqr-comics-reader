package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.download.DownloaderService
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.ui.adapters.downloads_queue.DownloadQueueAdapter
import com.tiagohs.hqr.ui.adapters.downloads_queue.DownloadQueueHolder
import com.tiagohs.hqr.ui.adapters.downloads_queue.DownloadQueueItem
import com.tiagohs.hqr.ui.callbacks.IDownloadQueuerListener
import com.tiagohs.hqr.ui.contracts.DownloadManagerContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_download_manager.*
import javax.inject.Inject

class DownloadManagerFragment: BaseFragment(), DownloadManagerContract.IDownloadManagerView, IDownloadQueuerListener {

    companion object {
        fun newFragment(): DownloadManagerFragment = DownloadManagerFragment()
    }

    override fun getViewID(): Int = R.layout.fragment_download_manager

    @Inject lateinit var presenter: DownloadManagerContract.IDownloadManagerPresenter

    private var adapter: DownloadQueueAdapter? = null
    private var isRunning: Boolean = false

    override fun onError(ex: Throwable, message: Int, withAction: Boolean) {
        setInformationViewStatus()

        super.onError(ex, message, withAction)
    }

    override fun onErrorAction() {
        presenter.onCreate()

        dismissSnack()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        activityCallbacks!!.setScreenTitle(getString(R.string.download_manager_title))

        getApplicationComponent()?.inject(this)

        setInformationViewStatus()

        adapter = DownloadQueueAdapter(this)
        downloadsQueueList.adapter = adapter
        downloadsQueueList.layoutManager = LinearLayoutManager(view.context)

        presenter.onBindView(this)
        presenter.onCreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.onUnbindView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_download_manager, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.actionStartDownload).isVisible = !isRunning && !presenter.isQueueEmpty()
        menu.findItem(R.id.actionPauseDownload).isVisible = isRunning
        menu.findItem(R.id.actionClearDownloads).isVisible = !presenter.isQueueEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = activity?.applicationContext ?: return false

        when (item.itemId) {
            R.id.actionStartDownload -> DownloaderService.startDownloaderService(context)
            R.id.actionPauseDownload -> {
                DownloaderService.stopDownloaderService(context)
                presenter.pauseDownloads()
            }
            R.id.actionClearDownloads -> {
                DownloaderService.stopDownloaderService(context)
                presenter.clearQueue()
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        val item = adapter.getItem(position) ?: return false

        startActivity(ComicDetailsActivity.newIntent(context, item.download.comic.pathLink!!))

        return false
    }

    override fun onQueueStatusChange(running: Boolean) {
       isRunning = running
        activity?.invalidateOptionsMenu()

        setInformationViewStatus()
    }

    override fun onUpdateProgress(download: Download) {
        getHolder(download)?.onNotifyProgress()
    }

    override fun onNextDownloads(downloads: ArrayList<DownloadQueueItem>) {
        activity?.invalidateOptionsMenu()
        setInformationViewStatus()
        adapter?.updateDataSet(downloads)
    }

    override fun onProgressChange(download: Download) {
        getHolder(download)?.onNotifyDownloadPages()
    }

    private fun getHolder(download: Download): DownloadQueueHolder? {
        return downloadsQueueList?.findViewHolderForItemId(download.chapter.id) as? DownloadQueueHolder
    }

    fun setInformationViewStatus() {

        if (presenter.isQueueEmpty()) {
            emptyView.show(R.drawable.ic_file_download_grey_128dp, R.string.no_downloads_in_queue)
        } else {
            emptyView.hide()
        }

    }
}