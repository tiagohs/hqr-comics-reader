package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.utils.ImageUtils
import kotlinx.android.synthetic.main.activity_comic_details.view.*

class ComicsListAdapter(private val comics: List<ComicsItem>,
                        private val context: Context?,
                        private val callback: IComicListCallback) : RecyclerView.Adapter<ComicsListAdapter.ComicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comic, parent, false)
        return ComicViewHolder(view, callback)
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

        val comicTitle = itemView.comicTitle
        val comicsImage = itemView.comicImage

        constructor(itemView: View, callback: IComicListCallback) : this(itemView) {
            this.callback = callback

            itemView.setOnClickListener(this)
        }

        fun onBindView(c: ComicsItem) {
            comic = c

            comicTitle.text = comic.title
            ImageUtils.load(comicsImage,
                    "https://hqbr.com.br/" + comic.imagePath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    true)
        }

        override fun onClick(p0: View?) {
            callback.onComicSelect(comic)
        }

    }
}