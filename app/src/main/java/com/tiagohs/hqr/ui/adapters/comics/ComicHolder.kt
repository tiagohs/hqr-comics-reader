package com.tiagohs.hqr.ui.adapters.comics

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.AnimationUtils
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_comic_simple_it.view.*

class ComicHolder(
        private val view: View,
        private val adapter: ComicsListAdapter
): BaseFlexibleViewHolder(view, adapter) {

    private val comicTitle = view.findViewById<TextView>(R.id.comicTitle)
    private val comicImage = view.findViewById<ImageView>(R.id.comicImage)
    private val addToFavBtn = view.findViewById<ToggleButton>(R.id.addToFavBtn)

    fun bind(comicItem: ComicItem) {
        val comic = comicItem.comic

        comicTitle.text = comic.name

        if (comicImage != null && !comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(comicImage,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    true)
        }

        onConfigurePublisher(comic.publisher)
        onConfigureStatus(comic.status)
        onConfigureFavoriteBtn(comic)
    }

    private fun onConfigureStatus(status: String?) {
        if (view.comicSStatus != null && status != null) {
            view.comicSStatus.text = ScreenUtils.getComicStatusText(view.context, status)
            view.comicSStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(view.context, status))
        } else if (view.comicSStatus != null) {
            view.comicSStatus.visibility = View.GONE
        }
    }

    private fun onConfigureFavoriteBtn(comic: ComicViewModel) {
        AnimationUtils.createScaleButtonAnimation(addToFavBtn)

        addToFavBtn.isChecked = comic.favorite
        addToFavBtn.setOnClickListener({ adapter.addOrRemoveFromFavorite(comic) })
    }

    private fun onConfigurePublisher(publisher: List<DefaultModelView>?) {
        if (view.comicPublisher != null && publisher != null) {
            view.comicPublisher.text = publisher.map { it.name }.joinToString(", ")
        } else if (view.comicPublisher != null) {
            view.comicPublisher.visibility = View.GONE
        }
    }

}