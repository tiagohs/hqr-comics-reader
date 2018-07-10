package com.tiagohs.hqr.ui.views.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.tools.EndlessRecyclerView
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.comics.ComicHolder
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.adapters.comics.ComicsListAdapter
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
    var scrollListener: EndlessRecyclerView? = null

    var mQuery: String? = null

    private var isSearch: Boolean = false
    private var isSearching: Boolean = false
    private var timer = Timer()
    private val DELAY: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)

        onConfigureRecyclerView()

        setViewInfomation()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onUnbindView()
    }

    private fun onConfigureRecyclerView() {
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listComicsAdapter = ComicsListAdapter(emptyList(), this)

        comicsListRecyclerView.layoutManager = layoutManager
        comicsListRecyclerView.adapter = listComicsAdapter

        scrollListener = createOnScrollListener()
        comicsListRecyclerView.addOnScrollListener(scrollListener)
        comicsListRecyclerView.setNestedScrollingEnabled(false)

        comicsListRecyclerView.layoutManager
    }

    override fun onBindComics(comics: List<ComicItem>?) {
        listComicsAdapter?.updateDataSet(comics)

        isSearch = true
        searchProgress.visibility = android.view.View.GONE

        setViewInfomation(listComicsAdapter?.items?.isNotEmpty() ?: false)
    }

    override fun onBindMoreComics(comics: List<ComicItem>?) {
        listComicsAdapter?.onAddMoreItems(comics)

        isSearch = true
        setViewInfomation(listComicsAdapter?.items?.isNotEmpty() ?: false)
    }

    override fun onBindItem(comic: ComicItem) {
        val position = listComicsAdapter?.indexOf(comic) ?: return

        listComicsAdapter?.updateItem(position, comic, null)
        listComicsAdapter?.notifyItemChanged(position)
    }

    private fun getHolder(comic: ComicItem): ComicHolder? {
        return comicsListRecyclerView?.findViewHolderForItemId(comic.comic.id) as? ComicHolder
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        presenter.addOrRemoveFromFavorite(comic)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val comic = listComicsAdapter?.getItem(position) ?: return false
        startActivity(ComicDetailsActivity.newIntent(this, comic.comic.pathLink!!))

        return true
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

    private fun createOnScrollListener(): EndlessRecyclerView {
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

        if (!mQuery.isNullOrEmpty()) {
            timer.cancel()
            timer = Timer()
            timer.schedule( object : TimerTask() {
                override fun run() {
                    this@SearchActivity.runOnUiThread {
                        onReset()

                        isSearch = false
                        isSearching = true
                        searchProgress.visibility = android.view.View.VISIBLE

                        setViewInfomation()
                    }

                    presenter.onReset()
                    presenter.onSearchComics(mQuery!!)
                }
            }, DELAY)
        } else {
            onReset()

            isSearch = false
            isSearching = false

            setViewInfomation()
        }

        return true
    }

    private fun onReset() {
        listComicsAdapter?.clear()
        listComicsAdapter?.notifyDataSetChanged()
        scrollListener?.onReset()
    }

    private fun setViewInfomation(hasResults: Boolean = false) {

        if (isSearch && !hasResults) {
            searchEmptyView.show(R.drawable.ic_search_dont_found_grey_128dp, R.string.no_found)
        } else if (isSearching) {
            searchEmptyView.hide()
        } else {
            searchProgress.visibility = android.view.View.GONE
            searchEmptyView.show(R.drawable.ic_search_eye_grey_128dp, R.string.search_info)
        }
    }

}