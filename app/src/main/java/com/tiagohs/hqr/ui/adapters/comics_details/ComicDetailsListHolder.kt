package com.tiagohs.hqr.ui.adapters.comics_details

import android.view.View
import android.widget.PopupMenu
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.DateUtils
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.*
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import kotlinx.android.synthetic.main.item_comic_detail.view.*

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
        onConfigureHistory(history, comic)
    }


    private fun onConfigureStatus(status: String?) {
        if (view.comicDetailStatus != null && !status.isNullOrEmpty() && !status.equals(UNKNOWN)) {
            view.comicDetailStatus.text = ScreenUtils.getComicStatusText(view.context, status)
            view.comicDetailStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(view.context, status))
        } else {
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

    private fun onConfigureHistory(history: ComicHistoryViewModel?, comic: ComicViewModel) {
        if (!adapter.showHistory) {
            view.comicDetailHistoryContainer.visibility = View.GONE
            return
        }

        if (history == null) {
            view.resumeDetailBtn.text = view.context.getString(R.string.read)

            view.comicDetailHistoryTime.visibility = View.GONE
            view.comicDetailHistoryChapter.visibility = View.GONE
        } else {
            view.comicDetailHistoryTime.text = DateUtils.formateDate(history.lastTimeRead!!)
            view.resumeDetailBtn.text = view.context.getString(R.string.resume)

            view.comicDetailHistoryChapter.text = history.chapter?.chapterName
        }

        view.resumeDetailBtn.setOnClickListener {
            val chapterViewModel: ChapterViewModel?

            if (history == null) {
                chapterViewModel = comic.chapters?.last()
            } else {
                chapterViewModel = history.chapter
            }

            if (chapterViewModel != null) {
                view.context.startActivity(ReaderActivity.newIntent(view.context, chapterViewModel.chapterPath!!, comic.pathLink!!))
            }
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