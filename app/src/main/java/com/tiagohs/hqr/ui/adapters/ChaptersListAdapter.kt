package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.callbacks.IChapterItemCallback
import kotlinx.android.synthetic.main.item_chapter.view.*

class ChaptersListAdapter(private val comic: ComicViewModel,
                          private val context: Context?,
                          private val callback: IChapterItemCallback) : RecyclerView.Adapter<ChaptersListAdapter.ChaptersItemrViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersItemrViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false)
        return ChaptersItemrViewHolder(view, callback)
    }

    override fun getItemCount(): Int {
        return comic.chapters!!.size
    }

    override fun onBindViewHolder(holder: ChaptersItemrViewHolder, position: Int) {
        holder.onBindItem(comic.chapters!![position], comic.name!!)
    }

    class ChaptersItemrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        lateinit var callback: IChapterItemCallback
        lateinit var chapter: ChapterViewModel

        val chapterTitle = itemView.chapterTitle
        val comicTitle = itemView.comicTitle
        val downloadContainer = itemView.donwloadIcon

        constructor(itemView: View, callback: IChapterItemCallback) : this(itemView) {
            this.callback = callback

            itemView.setOnClickListener(this)
        }

        fun onBindItem(chapter: ChapterViewModel, comicName: String) {
            this.chapter = chapter

            chapterTitle.text = chapter.chapterName
            comicTitle.text = comicName

            /*if (chapter.isDownloaded) {
                downloadContainer.visibility = View.VISIBLE
            } else {
                downloadContainer.visibility = View.GONE
            }*/

            downloadContainer.setOnClickListener(onGenreSelect(chapter))
        }

        fun onGenreSelect(chapter: ChapterViewModel): View.OnClickListener {
            return object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    callback.onDownloadSelect(chapter)
                }
            }
        }

        override fun onClick(p0: View?) {
            callback.onChapterSelect(chapter)
        }
    }
}