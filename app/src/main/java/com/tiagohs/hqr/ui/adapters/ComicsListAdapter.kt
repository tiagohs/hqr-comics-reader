package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.utils.ImageUtils
import com.tiagohs.hqr.utils.ScreenUtils
import kotlinx.android.synthetic.main.activity_comic_details.view.*
import kotlinx.android.synthetic.main.item_comic_simple_it.view.*
import kotlinx.android.synthetic.main.placeholder_comic_it_horizontal.view.*

class ComicsListAdapter(var comics: List<ComicsItem>,
                        private val context: Context?,
                        private val callback: IComicListCallback,
                        private val layoutId: Int) : RecyclerView.Adapter<ComicsListAdapter.ComicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return ComicViewHolder(view, callback, context!!)
    }

    override fun getItemCount(): Int {
        return comics.size
    }

    override fun onBindViewHolder(holder: ComicViewHolder, position: Int) {
        val comic = comics[position]

        holder.onBindView(comic)
    }

    class ComicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        lateinit var comic: ComicsItem
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

        fun onBindView(c: ComicsItem) {
            comic = c

            comicTitle.text = comic.title

            if (::comicsImage.isInitialized && ::placeholder.isInitialized) {
                ImageUtils.load(comicsImage,
                        "https://hqbr.com.br/" + comic.imagePath,
                        R.drawable.img_placeholder, true, placeholder)
            } else if (::comicsImage.isInitialized) {
                ImageUtils.load(comicsImage,
                        "https://hqbr.com.br/" + comic.imagePath,
                        R.drawable.img_placeholder,
                        R.drawable.img_placeholder,
                        true)
            }

            if (c.status.isNotEmpty() && ::comicsStatus.isInitialized) {
                comicsStatus.text = ScreenUtils.getComicStatusText(context, c.status)
                comicsStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(context, comic.status))
            } else if (::comicsStatus.isInitialized) {
                comicsStatus.visibility = View.GONE
            }

            if (c.publisher.isNotEmpty() && ::comicsPublisher.isInitialized) {
                comicsPublisher.text = c.publisher
            } else if (::comicsPublisher.isInitialized) {
                comicsPublisher.visibility = View.GONE
            }
        }

        override fun onClick(p0: View?) {
            callback.onComicSelect(comic)
        }

    }
}