package com.tiagohs.hqr.ui.adapters.comics_details

import android.view.View
import android.widget.PopupMenu
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.ComicHistoryViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_comic_detail.view.*
import kotlinx.android.synthetic.main.item_comic_simple_it.view.*

class ComicDetailsListHolder(
        val view: View,
        val adapter: ComicDetailsListAdapter
): BaseFlexibleViewHolder(view, adapter) {

    init {

        view.moreBtn.setOnClickListener { it.post { showPopUpMenu(it) } }
    }

    fun bind(favoriteItem: ComicDetailsListItem) {
        val comic = favoriteItem.comic
        val history = favoriteItem.history

        if (comic.posterPath != null && !comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(view.comicDetailImage,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    true)
        }

        view.comicDetailTitle.text = comic.name
        view.comicDetailSource.text = view.context.getString(R.string.source_detail_name, favoriteItem.getLanguage(), favoriteItem.getSourceName())

        onConfigurePublisher(comic.publisher)
        onConfigureStatus(comic.status)
        onConfigureHistory(history)
    }


    private fun onConfigureStatus(status: String?) {
        if (view.comicDetailStatus != null && status != null) {
            view.comicDetailStatus.text = ScreenUtils.getComicStatusText(view.context, status)
            view.comicDetailStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(view.context, status))
        } else if (view.comicSStatus != null) {
            view.comicDetailStatus.visibility = View.GONE
        }
    }

    private fun onConfigurePublisher(publisher: List<DefaultModelView>?) {
        if (view.comicDetailPublisher != null && publisher != null) {
            view.comicDetailPublisher.text = publisher.map { it.name }.joinToString(", ")
        } else if (view.comicDetailPublisher != null) {
            view.comicDetailPublisher.visibility = View.GONE
        }
    }

    private fun onConfigureHistory(history: ComicHistoryViewModel?) {
        if (!adapter.showHistory || history == null) {
            view.comicDetailHistoryContainer.visibility = View.GONE
            return
        }

        view.comicDetailHistoryTime.text = DateUtils.formateDate("dd de MMM de yyyy", history.lastTimeRead!!)
        view.resumeDetailBtn.text = "Continuar"
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