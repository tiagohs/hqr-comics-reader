package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.ReaderModel
import com.tiagohs.hqr.ui.adapters.ChaptersListAdapter
import com.tiagohs.hqr.ui.callbacks.IChapterItemCallback
import com.tiagohs.hqr.ui.contracts.ComicChaptersContract
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_comic_chapters.*
import javax.inject.Inject

private const val COMIC = "comic_link"

class ComicChaptersFragment: BaseFragment(), IChapterItemCallback, ComicChaptersContract.IComicChaptersView {

    companion object {
        fun newFragment(comic: ComicViewModel): ComicChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(COMIC, comic)

            val fragment = ComicChaptersFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    @Inject
    lateinit var presenter: ComicChaptersContract.IComicChaptersPresenter

    lateinit var comic: ComicViewModel

    override fun getViewID(): Int {
        return R.layout.fragment_comic_chapters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comic = arguments!!.getParcelable(COMIC)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)
        presenter.onCreate(comic)

        chaptersList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        chaptersList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        chaptersList.adapter = ChaptersListAdapter(comic, context, this)
    }

    override fun onChapterSelect(chapter: ChapterViewModel) {
        startActivity(ReaderActivity.newIntent(context, ReaderModel(chapter.chapterPath!!, comic, chapter)))
    }

    override fun onDownloadSelect(chapter: ChapterViewModel) {
        presenter.downloadChapters(listOf(chapter))
    }

}