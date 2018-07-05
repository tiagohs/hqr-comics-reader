package com.tiagohs.hqr.ui.adapters.comics

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.AnimationUtils
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_comic_simple_it.view.*

class ComicHolder(
        private val view: View,
        private val adapter: ComicsListAdapter
): BaseFlexibleViewHolder(view, adapter) {

    private val comicTitle = containerViewHolder.findViewById<TextView>(R.id.comicTitle)
    private val comicImage = containerViewHolder.findViewById<ImageView>(R.id.comicImage)
    private val addToFavBtn = containerViewHolder.findViewById<ToggleButton>(R.id.addToFavBtn)

    fun bind(comicItem: ComicItem) {
        val comic = comicItem.comic

        comicTitle.text = comic.name

        if (comic.posterPath != null && !comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(comicImage,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    true)
        }

        if (containerViewHolder.comicSStatus != null && comic.status != null) {
            containerViewHolder.comicSStatus.text = ScreenUtils.getComicStatusText(containerViewHolder.context, comic.status)
            containerViewHolder.comicSStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(containerViewHolder.context, comic.status))
        } else if (containerViewHolder.comicSStatus != null) {
            containerViewHolder.comicSStatus.visibility = View.GONE
        }

        if (containerViewHolder.comicPublisher != null && comic.publisher != null) {
            containerViewHolder.comicPublisher.text = comic.publisher!!.map { it.name }.joinToString(", ")
        } else if (containerViewHolder.comicPublisher != null) {
            containerViewHolder.comicPublisher.visibility = View.GONE
        }

        AnimationUtils.createScaleButtonAnimation(addToFavBtn)

        addToFavBtn.isChecked = comic.favorite
        addToFavBtn.setOnClickListener({ adapter.addOrRemoveFromFavorite(comic) })
    }

}