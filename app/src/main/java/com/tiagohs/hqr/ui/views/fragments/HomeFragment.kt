package com.tiagohs.hqr.ui.views.fragments

import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.getResourceColor
import com.tiagohs.hqr.helpers.extensions.toast
import com.tiagohs.hqr.helpers.tools.EndlessRecyclerView
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.FETCH_ALL
import com.tiagohs.hqr.models.view_models.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.view_models.ListComicsModel
import com.tiagohs.hqr.ui.adapters.comics.ComicHolder
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.adapters.comics.ComicsListAdapter
import com.tiagohs.hqr.ui.adapters.publishers.PublisherItem
import com.tiagohs.hqr.ui.adapters.publishers.PublishersAdapter
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.ui.callbacks.IPublisherCallback
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.ListComicsActivity
import com.tiagohs.hqr.ui.views.activities.SearchActivity
import com.tiagohs.hqr.ui.views.activities.SourcesActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject


class HomeFragment : BaseFragment(), HomeContract.IHomeView {

    companion object Factory {
        fun newFragment(): HomeFragment = HomeFragment()
    }

    @Inject lateinit var homePresenter: HomeContract.IHomePresenter
    @Inject lateinit var localeUtils: LocaleUtils

    private lateinit var source: ISource

    private var lastestUpdatesAdapter: ComicsListAdapter? = null
    private var popularComicsAdapter: ComicsListAdapter? = null
    private var publisherAdapter: PublishersAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu!!.clear()
        inflater!!.inflate(R.menu.menu_home, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.menu_search -> startActivity(SearchActivity.newIntent(context))
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        homePresenter.onUnbindView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityCallbacks!!.setScreenTitle(getString(R.string.home_title))

        getApplicationComponent()!!.inject(this)

        homePresenter.onBindView(this)

        homePresenter.observeSourcesChanges()

        lastestComicsTitleContainer.setOnClickListener({ goToComicsListPage()})
        popularsComicsTitleContainer.setOnClickListener({ goToComicsListPage() })

        changeSource.setOnClickListener({ goToSources() })
    }

    override fun getViewID(): Int {
        return R.layout.fragment_home
    }

    override fun onBindSourceInfo(source: ISource) {
        this.source = source

        sourceName.text = source.name
        sourceUrl.text = source.baseUrl

        languageLogo.setImageDrawable(localeUtils.getLocaleImage(source.language, context))
        languageLogo.visibility = View.VISIBLE

        goToSiteButton.setOnClickListener { goToSourcePage(source.baseUrl) }
    }

    override fun onBindPublishers(publishers: List<PublisherItem>) {
        publisherAdapter = PublishersAdapter(onPublisherCallback())
        publisherAdapter?.updateDataSet(publishers)

        val publishersLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        publishersList.adapter = publisherAdapter
        publishersList.layoutManager = publishersLayoutManager

        publishersList.addOnScrollListener(createPublishersScrollListener(publishersLayoutManager))
        publishersList.setNestedScrollingEnabled(false)

        publishersListProgress.visibility = View.GONE
    }

    private fun createPublishersScrollListener(publishersLayoutManager: RecyclerView.LayoutManager): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(publishersLayoutManager) {
            override fun onLoadMore(current_page: Int) {
                homePresenter.onGetMorePublishers()
            }
        }
    }

    override fun onBindLastestUpdates(lastestUpdates: List<ComicItem>) {
        lastestUpdatesAdapter = ComicsListAdapter(lastestUpdates, onLastestCallback())

        val lastestLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        lastestList.adapter = lastestUpdatesAdapter
        lastestList.layoutManager = lastestLayoutManager

        lastestList.addOnScrollListener(createLastestScrollListener(lastestLayoutManager))
        lastestList.setNestedScrollingEnabled(false)

        lastestListProgress.visibility = View.GONE
    }

    private fun createLastestScrollListener(lastestLayoutManager: RecyclerView.LayoutManager): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(lastestLayoutManager) {
            override fun onLoadMore(current_page: Int) {
                homePresenter.onGetMoreLastestComics()
            }
        }
    }

    override fun onBindPopulars(populars: List<ComicItem>) {
        popularComicsAdapter = ComicsListAdapter(populars, onPopularsCallback())

        popularList.adapter = popularComicsAdapter

        val popularLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        popularList.layoutManager = popularLayoutManager

        popularList.addOnScrollListener(createPopularsScrollListener(popularLayoutManager))
        popularList.setNestedScrollingEnabled(false)

        popularListProgress.visibility = View.GONE
    }

    private fun createPopularsScrollListener(popularLayoutManager: RecyclerView.LayoutManager): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(popularLayoutManager) {
            override fun onLoadMore(current_page: Int) {
                homePresenter.onGetMorePopularComics()
            }
        }
    }

    private fun onPopularsCallback(): IComicListCallback {
        return object : IComicListCallback {
            override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
                homePresenter.addOrRemoveFromFavorite(comic)
            }

            override fun onItemClick(view: View?, position: Int): Boolean {
                val comic = popularComicsAdapter?.getItem(position) ?: return false
                startActivity(ComicDetailsActivity.newIntent(context, comic.comic.pathLink!!))

                return true
            }
        }
    }

    private fun onLastestCallback(): IComicListCallback {
        return object : IComicListCallback {
            override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
                homePresenter.addOrRemoveFromFavorite(comic)
            }

            override fun onItemClick(view: View?, position: Int): Boolean {
                val comic = lastestUpdatesAdapter?.getItem(position) ?: return false
                startActivity(ComicDetailsActivity.newIntent(context, comic.comic.pathLink!!))

                return true
            }
        }
    }

    override fun onBindMorePublishers(publishers: List<PublisherItem>) {
        publisherAdapter?.onAddMoreItems(publishers)
    }

    override fun onBindMorePopulars(populars: List<ComicItem>) {
        popularComicsAdapter?.onAddMoreItems(populars)
    }

    override fun onBindMoreLastestUpdates(lastestUpdates: List<ComicItem>) {
        lastestUpdatesAdapter?.onAddMoreItems(lastestUpdates)
    }

    private fun getPopularHolder(comic: ComicItem): ComicHolder? {
        return popularList?.findViewHolderForItemId(comic.comic.id) as? ComicHolder
    }

    private fun getLastestHolder(comic: ComicItem): ComicHolder? {
        return lastestList?.findViewHolderForItemId(comic.comic.id) as? ComicHolder
    }

    override fun onBindPopularItem(comic: ComicItem) {
        val position = popularComicsAdapter?.indexOf(comic) ?: return

        popularComicsAdapter?.updateItem(position, comic, null)
        popularComicsAdapter?.notifyItemChanged(position)
    }

    override fun onBindLastestItem(comic: ComicItem) {
        val position = lastestUpdatesAdapter?.indexOf(comic) ?: return

        lastestUpdatesAdapter?.updateItem(position, comic, null)
        lastestUpdatesAdapter?.notifyItemChanged(position)
    }

    private fun goToComicsListPage() {
        startActivity(ListComicsActivity.newIntent(context, ListComicsModel(FETCH_ALL, source.name!!, "")))
    }

    private fun goToSources() {
        startActivity(SourcesActivity.newIntent(context))
    }

    private fun goToSourcePage(baseUrl: String) {
        val context = view?.context ?: return

        try {
            val url = Uri.parse(baseUrl)
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(context.getResourceColor(R.color.colorPrimary))
                    .setShowTitle(true)
                    .build()
            intent.launchUrl(activity, url)
        } catch (e: Exception) {
            context.toast(e.message)
        }

    }

    private fun onPublisherCallback(): IPublisherCallback {
        return object : IPublisherCallback {
            override fun onItemClick(view: View?, position: Int): Boolean {
                val item = publisherAdapter?.getItem(position) ?: return false
                startActivity(ListComicsActivity.newIntent(context, ListComicsModel(FETCH_BY_PUBLISHERS, item.publisher.name!!, item.publisher.pathLink!!)))

                return true
            }
        }
    }

}