package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.ui.adapters.ComicsListAdapter
import com.tiagohs.hqr.ui.adapters.PublishersListAdapter
import com.tiagohs.hqr.ui.contracts.HomeContract
import com.tiagohs.hqr.ui.views.IComicListCallback
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject

class HomeFragment : BaseFragment(), HomeContract.IHomeView, IComicListCallback {

    companion object Factory {
        fun newFragment(): HomeFragment = HomeFragment()
    }

    @Inject lateinit var homePresenter: HomeContract.IHomePresenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()!!.inject(this)

        homePresenter.onBindView(this)

        homePresenter.onGetPublishers()
        homePresenter.onGetHomePageData()
    }

    override fun getViewID(): Int {
        return R.layout.fragment_home
    }

    override fun onComicSelect(comic: ComicsItem) {
        startActivity(ComicDetailsActivity.newIntent(context, comic.link))
    }

    override fun onBindPublishers(publishers: List<Publisher>) {
        publishersList.adapter = PublishersListAdapter(publishers, context)
        publishersList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)
    }

    override fun onBindLastestUpdates(lastestUpdates: List<ComicsItem>) {
        lastestList.adapter = ComicsListAdapter(lastestUpdates, context, this)
        lastestList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)
    }

    override fun onBindPopulars(populars: List<ComicsItem>) {
        popularList.adapter = ComicsListAdapter(populars, context, this)
        popularList.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL)
    }

    override fun isInternetConnected(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}