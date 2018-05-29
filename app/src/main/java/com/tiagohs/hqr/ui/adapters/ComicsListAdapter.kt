package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.utils.ImageUtils
import kotlinx.android.synthetic.main.item_comic.view.*

class ComicsListAdapter(private val comics: List<ComicsItem>,
                        private val context: Context?) : RecyclerView.Adapter<ComicsListAdapter.ComicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comic, parent, false)
        return ComicViewHolder(view)
    }

    override fun getItemCount(): Int {
        return comics.size
    }

    override fun onBindViewHolder(holder: ComicViewHolder, position: Int) {
        val comic = comics[position]

        holder.comicTitle.text = comic.title
        ImageUtils.load(holder.comicsImage,
                "https://hqbr.com.br/" + comic.imagePath,
                R.drawable.img_placeholder,
                R.drawable.img_placeholder,
                true)
    }

    class ComicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val comicTitle = itemView.comicTitle
        val comicsImage = itemView.comicImage
    }
}