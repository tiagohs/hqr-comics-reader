package com.tiagohs.hqr.ui.adapters.downloads

import android.view.View
import android.widget.PopupMenu
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.AnimationUtils
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.models.view_models.UNKNOWN
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_downloads.view.*

class DownloadHolder(
        private val view: View,
        private val adapter: DownloadsAdapter
): BaseFlexibleViewHolder(view, adapter) {

    init {
        view.comicDownloadedmoreBtn.setOnClickListener { it.post { showPopUpMenu(it) } }
    }

    fun bind(downloadItem: DownloadItem) {
        val comic = downloadItem.comic

        if (comic.posterPath != null && !comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(view.comicDownloadedImage,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    true)
        }

        view.comicDownloadedTitle.text = comic.name
        view.comicDownloadedSource.text = view.context.getString(R.string.source_detail_name, downloadItem.comic.source?.language, downloadItem.comic.source?.name)

        onConfigurePublisher(comic.publisher)
        onConfigureStatus(comic.status)
        onConfigureFavoriteBtn(downloadItem)

        view.comicDownloadedNumberOf.text = view.context.getString(R.string.download_number_of, downloadItem.chaptersDownloaded.size)
    }

    private fun onConfigureStatus(status: String?) {
        if (view.comicDownloadedStatus != null && !status.isNullOrEmpty() && !status.equals(UNKNOWN)) {
            view.comicDownloadedStatus.text = ScreenUtils.getComicStatusText(view.context, status)
            view.comicDownloadedStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(view.context, status))
        } else {
            view.comicDownloadedStatus.visibility = View.GONE
        }
    }

    private fun onConfigureFavoriteBtn(downloadItem: DownloadItem) {
        AnimationUtils.createScaleButtonAnimation(view.comicDownloadedAddToFavBtn)

        view.comicDownloadedAddToFavBtn.isChecked = downloadItem.comic.favorite
        view.comicDownloadedAddToFavBtn.setOnClickListener({ adapter.addOrRemoveFromFavorite(downloadItem) })
    }

    private fun onConfigurePublisher(publisher: List<DefaultModelView>?) {
        if (view.comicDownloadedPublisher != null && publisher != null) {
            view.comicDownloadedPublisher.text = publisher.map { it.name }.joinToString(", ")
        } else if (view.comicDownloadedPublisher != null) {
            view.comicDownloadedPublisher.visibility = View.GONE
        }
    }

    private fun showPopUpMenu(view: View) {
        val item = adapter.getItem(adapterPosition) ?: return
        val popup = PopupMenu(view.context, view)

        adapter.onMenuCreate(popup.menuInflater, popup.menu)
        adapter.onPrepareMenu(popup.menu, adapterPosition, item)

        popup.setOnMenuItemClickListener {
            adapter.onMenuClick(adapterPosition, it)
            true
        }

        popup.show()
    }
}