package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.viewModels.FETCH_ALL
import com.tiagohs.hqr.models.viewModels.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.viewModels.FETCH_BY_SCANLATORS
import com.tiagohs.hqr.models.viewModels.ListComicsModel
import com.tiagohs.hqr.ui.adapters.ComicsListAdapter
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.ui.contracts.ListComicsContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.SearchActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import com.tiagohs.hqr.utils.EndlessRecyclerView
import kotlinx.android.synthetic.main.fragment_list_comics.*
import javax.inject.Inject

const val LIST_COMICS_MODEL = "LIST_COMICS_MODEL"

class ListComicsFragment: BaseFragment(), ListComicsContract.IListComicsView, IComicListCallback {

    companion object {
        fun newFragment(listComicsModel: ListComicsModel): ListComicsFragment {
            val bundle = Bundle()
            bundle.putParcelable(LIST_COMICS_MODEL, listComicsModel)

            val fragment = ListComicsFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    override fun getViewID(): Int = R.layout.fragment_list_comics

    @Inject
    lateinit var presenter: ListComicsContract.IListComicsPresenter

    lateinit var listComicsModel: ListComicsModel
    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var listComicsAdapter: ComicsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listComicsModel = arguments!!.getParcelable(LIST_COMICS_MODEL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)

        when (listComicsModel.listType) {
            FETCH_ALL -> presenter.onGetComics(listComicsModel.listType, "All")
            FETCH_BY_PUBLISHERS, FETCH_BY_SCANLATORS -> presenter.onGetComics(listComicsModel.listType, listComicsModel.link)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu!!.clear()
        inflater!!.inflate(R.menu.menu_comics_list, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.menu_search -> {
                if (!presenter.hasPagesSupport()) {
                    startActivity(SearchActivity.newIntent(context))
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        presenter.onUnbindView()
    }

    override fun onBindComics(comics: List<ComicsItem>?) {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listComicsAdapter = ComicsListAdapter(comics!!, context, this, R.layout.item_comic_simple_it)

        comicsList.layoutManager = layoutManager
        comicsList.adapter = listComicsAdapter
        comicsList.addOnScrollListener(createOnScrollListener())
        comicsList.setNestedScrollingEnabled(false)
    }

    override fun onBindMoreComics(comics: List<ComicsItem>) {
        listComicsAdapter.comics = comics
        listComicsAdapter.notifyDataSetChanged()
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

    override fun onComicSelect(comic: ComicsItem) {
        startActivity(ComicDetailsActivity.newIntent(context, comic.link))
    }
}