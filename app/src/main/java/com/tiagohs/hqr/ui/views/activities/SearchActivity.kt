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
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.ui.adapters.ComicsListAdapter
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.ui.contracts.SearchContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.utils.EndlessRecyclerView
import kotlinx.android.synthetic.main.activity_search.*
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
    lateinit var listComicsAdapter: ComicsListAdapter

    var mQuery: String = ""

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

    override fun onBindComics(comics: List<ComicsItem>?) {
        listComicsAdapter.comics = comics!!
        listComicsAdapter.notifyDataSetChanged()
    }

    override fun onComicSelect(comic: ComicsItem) {
        startActivity(ComicDetailsActivity.newIntent(this, comic.link))
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
        mQuery = query!!

        if (!mQuery.isNullOrEmpty()) {
            presenter.onSearchComics(mQuery)
        }

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        mQuery = newText!!

        if (!mQuery.isNullOrEmpty()) {
            presenter.onSearchComics(mQuery)
        }

        return true
    }

}