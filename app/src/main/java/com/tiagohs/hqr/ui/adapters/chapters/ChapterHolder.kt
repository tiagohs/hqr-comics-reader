package com.tiagohs.hqr.ui.adapters.chapters

import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.getResourceDrawable
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_chapter.view.*

class ChapterHolder(
        private val view: View,
        private val adapter: ChaptersListAdapter
): BaseFlexibleViewHolder(view, adapter) {

    init {

    }

    fun bind(item: ChapterItem, comic: ComicViewModel) {
        view.chapterTitle.text = item.chapter.chapterName
        view.comicTitle.text = comic.name

        view.donwloadIconBackground.setOnClickListener { adapter.onDownloadButtonClicked(item, comic) }

        notifyStatus(item.status)
    }

    fun notifyStatus(status: String) {
        when (status) {
            Download.QUEUE, Download.DOWNLOADING -> onDownloading()
            Download.DOWNLOADED -> onDownloaded()
            Download.ERROR -> onDownloadError()
            Download.NOT_DOWNLOADED -> onNotDownloaded()
            else -> onNotDownloaded()
        }
    }

    private fun onDownloaded() {
        view.downloadIconContainer.visibility = View.VISIBLE
        view.donwloadIcon.setImageDrawable(view.context.getResourceDrawable(R.drawable.ic_download_white))
        view.donwloadIconBackground.visibility = View.VISIBLE
        view.downloadingProgress.visibility = View.GONE
    }

    private fun onDownloadError() {
        view.downloadIconContainer.visibility = View.VISIBLE
        view.donwloadIcon.setImageDrawable(view.context.getResourceDrawable(R.drawable.ic_error_outline_white))
        view.donwloadIconBackground.visibility = View.VISIBLE
        view.downloadingProgress.visibility = View.GONE
    }

    private fun onDownloading() {
        view.downloadIconContainer.visibility = View.VISIBLE
        view.donwloadIconBackground.visibility = View.GONE
        view.downloadingProgress.visibility = View.VISIBLE
    }

    private fun onNotDownloaded() {
        view.downloadIconContainer.visibility = View.GONE
        view.downloadingProgress.visibility = View.GONE
    }

}