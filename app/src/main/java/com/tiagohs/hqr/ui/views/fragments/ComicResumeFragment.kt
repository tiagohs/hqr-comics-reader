package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.models.viewModels.FETCH_BY_SCANLATORS
import com.tiagohs.hqr.models.viewModels.ListComicsModel
import com.tiagohs.hqr.ui.adapters.SimpleItemAdapter
import com.tiagohs.hqr.ui.callbacks.ISimpleItemCallback
import com.tiagohs.hqr.ui.views.activities.ListComicsActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_comic_resume.*

private const val COMIC = "comic_link"

class ComicResumeFragment: BaseFragment() {

    companion object {
        fun newFragment(comic: ComicViewModel): ComicResumeFragment {
            val bundle = Bundle()
            bundle.putParcelable(COMIC, comic)

            val fragment = ComicResumeFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    lateinit var comic: ComicViewModel

    override fun getViewID(): Int {
        return R.layout.fragment_comic_resume
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comic = arguments!!.getParcelable(COMIC)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (comic.publicationDate != null) datePublish.text = comic.publicationDate
        else datePublishContainer.visibility = View.GONE

        onBindList(comic.authors, authorsList, onAuthorSelect())
        onBindList(comic.genres, genresList, onGenreSelect())
        onBindList(comic.scanlators, scanlatorsList, onScanlatorSelect())

        if (comic.summary != null) summary.text = comic.summary
        else summary.text = "Sinopse Indisponível"
    }

    private fun onBindList(list: List<DefaultModel>?, listItem: RecyclerView, callback: ISimpleItemCallback) {
        if (list!!.isNotEmpty()) {
            listItem.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            listItem.adapter = SimpleItemAdapter(list, context, callback)
        } else listItem.visibility = android.view.View.GONE
    }

    fun onAuthorSelect(): ISimpleItemCallback {
        return object : ISimpleItemCallback {
            override fun onClick(item: DefaultModel) {
                Log.d("ComicDetails", "onAuthorSelect")
            }
        }
    }

    fun onGenreSelect(): ISimpleItemCallback {
        return object : ISimpleItemCallback {
            override fun onClick(item: DefaultModel) {
                Log.d("ComicDetails", "onGenreSelect")
            }
        }
    }

    fun onScanlatorSelect(): ISimpleItemCallback {
        return object : ISimpleItemCallback {
            override fun onClick(item: DefaultModel) {
                startActivity(ListComicsActivity.newIntent(context, ListComicsModel(FETCH_BY_SCANLATORS, item.name!!, item.pathLink!!)))
            }
        }
    }
}