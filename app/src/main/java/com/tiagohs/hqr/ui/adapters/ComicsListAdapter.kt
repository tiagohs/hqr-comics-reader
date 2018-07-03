package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import kotlinx.android.synthetic.main.activity_comic_details.view.*
import kotlinx.android.synthetic.main.item_comic_simple_it.view.*
import kotlinx.android.synthetic.main.placeholder_comic_it_horizontal.view.*

class ComicsListAdapter(var comics: List<ComicViewModel>,
                        private val context: Context?,
                        private val callback: IComicListCallback,
                        private val layoutId: Int) : RecyclerView.Adapter<ComicsListAdapter.ComicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return ComicViewHolder(view, callback, context!!)
    }

    fun getComic(comic: ComicViewModel): ComicViewModel? {
        return comics.find {it.pathLink == comic.pathLink }
    }

    fun getComicIndex(comic: ComicViewModel): Int? {
        var index: Int? = null

        for ((i, it) in comics.withIndex()) {
            if (it.pathLink == comic.pathLink) {
                index = i
                break
            }
        }

        return index
    }

    override fun getItemCount(): Int {
        return comics.size
    }

    override fun onBindViewHolder(holder: ComicViewHolder, position: Int) {
        val comic = comics[position]

        holder.onBindView(comic)
    }

    class ComicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        lateinit var comic: ComicViewModel
        lateinit var callback: IComicListCallback
        lateinit var context: Context

        lateinit var comicTitle: TextView
        lateinit var comicsImage: ImageView
        lateinit var comicsStatus: TextView
        lateinit var comicsPublisher: TextView
        lateinit var placeholder: View

        constructor(itemView: View, callback: IComicListCallback, context: Context) : this(itemView) {
            this.callback = callback
            this.context = context

            itemView.setOnClickListener(this)

            if (itemView.comicSTitle != null)
                comicTitle = itemView.comicSTitle
            else if (itemView.comicTitle != null)
                comicTitle = itemView.comicTitle

            if (itemView.comicSStatus != null)
                comicsStatus = itemView.comicSStatus
            else if (itemView.comicStatus != null)
                comicsStatus = itemView.comicStatus

            if (itemView.comicImage != null)
                comicsImage = itemView.comicImage

            if (itemView.comicPublisher != null)
                comicsPublisher = itemView.comicPublisher

            if (itemView.placeholderItem != null)
                placeholder = itemView.placeholderItem
        }

        fun onBindView(c: ComicViewModel) {
            comic = c

            comicTitle.text = comic.name

            if (::comicsImage.isInitialized && !comic.posterPath.isNullOrEmpty()) {
                ImageUtils.load(comicsImage,
                        comic.posterPath,
                        R.drawable.img_placeholder,
                        R.drawable.img_placeholder,
                        true)
            }

            if (c.status != null && ::comicsStatus.isInitialized) {
                comicsStatus.text = ScreenUtils.getComicStatusText(context, c.status)
                comicsStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(context, comic.status))
            } else if (::comicsStatus.isInitialized) {
                comicsStatus.visibility = View.GONE
            }

            if (c.publisher != null && ::comicsPublisher.isInitialized) {
                comicsPublisher.text = c.publisher!!.map { it.name }.joinToString(", ")
            } else if (::comicsPublisher.isInitialized) {
                comicsPublisher.visibility = View.GONE
            }
        }

        override fun onClick(p0: View?) {
            callback.onComicSelect(comic)
        }

    }
}