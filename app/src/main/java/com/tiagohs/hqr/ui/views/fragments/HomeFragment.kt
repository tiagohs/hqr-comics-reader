package com.tiagohs.hqr.ui.views.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.base.ISource
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.FETCH_ALL
import com.tiagohs.hqr.models.view_models.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.view_models.ListComicsModel
import com.tiagohs.hqr.ui.adapters.ComicsListAdapter
import com.tiagohs.hqr.ui.adapters.PublishersListAdapter
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


class HomeFragment : BaseFragment(), HomeContract.IHomeView, IComicListCallback {

    companion object Factory {
        fun newFragment(): HomeFragment = HomeFragment()
    }

    @Inject lateinit var homePresenter: HomeContract.IHomePresenter
    @Inject lateinit var localeUtils: LocaleUtils

    private lateinit var source: ISource

    private var lastestUpdatesAdapter: ComicsListAdapter? = null
    private var popularComicsAdapter: ComicsListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityCallbacks!!.setScreenTitle("Ínicio")
    }

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

    override fun onComicSelect(comic: ComicViewModel) {
        startActivity(ComicDetailsActivity.newIntent(context, comic.pathLink!!))
    }

    override fun onBindSourceInfo(source: ISource) {
        this.source = source

        sourceName.text = source.name
        sourceUrl.text = source.baseUrl

        languageLogo.setImageDrawable(localeUtils.getLocaleImage(source.language, context))
        languageLogo.visibility = View.VISIBLE

        goToSiteButton.setOnClickListener { goToSourcePage(source.baseUrl) }
    }

    override fun onBindPublishers(publishers: List<Publisher>) {
        publishersList.adapter = PublishersListAdapter(publishers, context, onPublisherCallback())
        publishersList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)

        publishersListProgress.visibility = View.GONE
    }

    override fun onBindLastestUpdates(lastestUpdates: List<ComicViewModel>) {
        lastestUpdatesAdapter = ComicsListAdapter(lastestUpdates, context, this, R.layout.item_comic)

        lastestList.adapter = lastestUpdatesAdapter
        lastestList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)

        lastestListProgress.visibility = View.GONE
    }

    override fun onBindPopulars(populars: List<ComicViewModel>) {
        popularComicsAdapter = ComicsListAdapter(populars, context, this, R.layout.item_comic)

        popularList.adapter = popularComicsAdapter
        popularList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)

        popularListProgress.visibility = View.GONE
    }

    override fun onBindPopularItem(comic: ComicViewModel) {
        if (popularComicsAdapter != null) {
            val c = popularComicsAdapter!!.getComic(comic)

            if (c != null) {
                c.copyFrom(comic)
                val index = popularComicsAdapter!!.getComicIndex(comic)

                if (index != null) {
                    popularComicsAdapter!!.notifyItemChanged(index)
                }

            }
        }
    }

    override fun onBindLastestItem(comic: ComicViewModel) {

        if (lastestUpdatesAdapter != null) {
            val c = lastestUpdatesAdapter!!.getComic(comic)

            if (c != null) {
                c.copyFrom(comic)
                val index = lastestUpdatesAdapter!!.getComicIndex(comic)

                if (index != null) {
                    lastestUpdatesAdapter!!.notifyItemChanged(index)
                }
            }
        }
    }

    private fun goToComicsListPage() {
        startActivity(ListComicsActivity.newIntent(context, ListComicsModel(FETCH_ALL, "HQS - HQBR", "")))
    }

    private fun goToSources() {
        startActivity(SourcesActivity.newIntent(context))
    }

    private fun goToSourcePage(baseUrl: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl))
        startActivity(browserIntent)
    }

    private fun onPublisherCallback(): IPublisherCallback {
        return object : IPublisherCallback {
            override fun onClick(item: Publisher) {
                startActivity(ListComicsActivity.newIntent(context, ListComicsModel(FETCH_BY_PUBLISHERS, item.name, item.url)))
            }
        }
    }

}