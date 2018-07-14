package com.tiagohs.hqr.ui.adapters.downloads_queue

import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_download_manager_list.view.*

class DownloadQueueHolder(
        view: View,
        adapter: DownloadQueueAdapter
): BaseFlexibleViewHolder(view, adapter) {

    private lateinit var download: Download

    fun bind(download: Download) {
        this.download = download

        if (!download.comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(containerViewHolder.comicPoster,
                    download.comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    false)
        }

        containerViewHolder.chapterTitle.text = download.chapter.chapterName
        containerViewHolder.comicTitle.text = download.comic.name
        containerViewHolder.sourceTitle.text = containerViewHolder.context.getString(R.string.source_detail_name, download.sourceDB.language, download.sourceDB.name)

        val pages = download.chapter.pages

        if (pages == null) {
            containerViewHolder.downloadProgress.progress = 0
            containerViewHolder.downloadProgress.max = 1
            containerViewHolder.downloadStatus.text = ""
        } else {
            containerViewHolder.downloadProgress.max = pages.size * 100
            onNotifyProgress()
            onNotifyDownloadPages()
        }
    }

    fun onNotifyProgress() {
        val pages = download.chapter.pages ?: return

        if (containerViewHolder.downloadProgress.max == 1) {
            containerViewHolder.downloadProgress.max = pages.size * 100
        }

        containerViewHolder.downloadProgress.progress = download.progressTotal
    }

    fun onNotifyDownloadPages() {
        val pages = download.chapter.pages ?: return
        containerViewHolder.downloadStatus.text = containerViewHolder.context.getString(R.string.download_status, download.numberOfImagesDownloaded, pages.size)
    }

}