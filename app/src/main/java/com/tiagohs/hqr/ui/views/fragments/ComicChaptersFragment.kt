package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.models.viewModels.ReaderModel
import com.tiagohs.hqr.ui.adapters.ChaptersListAdapter
import com.tiagohs.hqr.ui.callbacks.IChapterItemCallback
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_comic_chapters.*

private const val COMIC = "comic_link"

class ComicChaptersFragment: BaseFragment(), IChapterItemCallback {

    companion object {
        fun newFragment(comic: Comic): ComicChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(COMIC, comic)

            val fragment = ComicChaptersFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    lateinit var comic: Comic

    override fun getViewID(): Int {
        return R.layout.fragment_comic_chapters
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comic = arguments!!.getParcelable(COMIC)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chaptersList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        chaptersList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        chaptersList.adapter = ChaptersListAdapter(comic, context, this)
    }

    override fun onChapterSelect(chapter: Chapter) {
        startActivity(ReaderActivity.newIntent(context, ReaderModel(chapter.chapterPath!!, comic, chapter)))
    }

    override fun onDownloadSelect(chapter: Chapter) {
        Log.d("ComicDetails", "onDownloadSelect")
    }

}