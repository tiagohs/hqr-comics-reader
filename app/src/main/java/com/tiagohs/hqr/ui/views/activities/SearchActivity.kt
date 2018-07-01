package com.tiagohs.hqr.ui.views.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.tools.EndlessRecyclerView
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.ComicsListAdapter
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.ui.contracts.SearchContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*
import javax.inject.Inject

class SearchActivity : BaseActivity(), SearchView.OnQueryTextListener, IComicListCallback, SearchContract.ISearchView {

    companion object {
        fun newIntent(context: Context?): Intent {
            return  Intent(context, SearchActivity::class.java)
        }
    }

    override fun onGetMenuLayoutId(): Int = R.menu.menu_search_screen
    override fun onGetLayoutViewId(): Int  = R.layout.activity_search

    @Inject
    lateinit var presenter: SearchContract.ISearchPresenter

    lateinit var mSearchView: SearchView
    lateinit var layoutManager: RecyclerView.LayoutManager

    var listComicsAdapter: ComicsListAdapter? = null

    var mQuery: String? = null

    private var timer = Timer()
    private val DELAY: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)

        onConfigureRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onUnbindView()
    }

    private fun onConfigureRecyclerView() {
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listComicsAdapter = ComicsListAdapter(ArrayList(), this, this, R.layout.item_comic_simple_it)

        comicsListRecyclerView.layoutManager = layoutManager
        comicsListRecyclerView.adapter = listComicsAdapter

        comicsListRecyclerView.addOnScrollListener(createOnScrollListener())
        comicsListRecyclerView.setNestedScrollingEnabled(false)

        comicsListRecyclerView.layoutManager
    }

    override fun onBindComics(comics: List<ComicViewModel>?) {
        listComicsAdapter?.comics = comics!!
        listComicsAdapter?.notifyDataSetChanged()
    }

    override fun onBindItem(comic: ComicViewModel) {
        if (listComicsAdapter != null) {
            val c = listComicsAdapter!!.getComic(comic)

            if (c != null) {
                c.copyFrom(comic)
                val index = listComicsAdapter!!.getComicIndex(comic)

                if (index != null) {
                    listComicsAdapter!!.notifyItemChanged(index)
                }

            }
        }
    }

    override fun onComicSelect(comic: ComicViewModel) {
        startActivity(ComicDetailsActivity.newIntent(this, comic.pathLink!!))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchView = menu.findItem(R.id.menu_search).actionView as SearchView

        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()))
        mSearchView.setQueryHint(getString(R.string.action_procurar))
        mSearchView.setIconifiedByDefault(false)
        mSearchView.setOnQueryTextListener(this)
        mSearchView.setFocusable(true)
        mSearchView.requestFocus()
        mSearchView.setQuery(if (!mQuery.isNullOrEmpty()) mQuery else "", false)

        return true
    }

    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(layoutManager) {

            override fun onLoadMore(current_page: Int) {
                if (presenter.hasMoreComics()) {
                    presenter.onGetMoreComics()
                }
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return onTextChange(query)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        return onTextChange(query)
    }

    private fun onTextChange(query: String?): Boolean {
        mQuery = query

        if (mQuery != null && mQuery!!.isNotEmpty()) {
            timer.cancel()
            timer = Timer()
            timer.schedule( object : TimerTask() {
                override fun run() {
                    presenter.onSearchComics(mQuery!!)
                }
            }, DELAY)
        }

        return true
    }

}