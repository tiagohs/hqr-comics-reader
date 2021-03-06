package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.tools.EndlessRecyclerView
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.*
import com.tiagohs.hqr.ui.adapters.comics.ComicItem
import com.tiagohs.hqr.ui.adapters.comics.ComicsListAdapter
import com.tiagohs.hqr.ui.callbacks.IComicListCallback
import com.tiagohs.hqr.ui.contracts.ListComicsContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.SearchActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
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

    var listComicsAdapter: ComicsListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listComicsModel = arguments!!.getParcelable(LIST_COMICS_MODEL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)

        onInit()
    }

    private fun onInit() {
        when (listComicsModel.listType) {
            FETCH_ALL -> presenter.onGetComics(listComicsModel.listType, "All")
            FETCH_BY_PUBLISHERS, FETCH_BY_SCANLATORS, FETCH_BY_GENRES -> presenter.onGetComics(listComicsModel.listType, listComicsModel.link)
        }
    }
    override fun onError(ex: Throwable, message: Int, withAction: Boolean) {
        comicListProgress.visibility = View.GONE

        super.onError(ex, message, withAction)
    }

    override fun onErrorAction() {
        comicListProgress.visibility = View.VISIBLE

        onInit()

        dismissSnack()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu!!.clear()
        inflater!!.inflate(R.menu.menu_comics_list, menu)

        //menu.findItem(R.id.menu_filter)?.isVisible = listComicsAdapter != null && !listComicsAdapter?.isEmpty!!
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

    override fun onBindComics(comics: List<ComicItem>?) {
        layoutManager = GridLayoutManager(context, ScreenUtils.calculateNoOfColumns(context, resources.getDimension(R.dimen.item_comic_default_width)))

        listComicsAdapter = ComicsListAdapter( comics!!, this)
        listComicsAdapter?.updateDataSet(comics)

        comicsList.layoutManager = layoutManager
        comicsList.adapter = listComicsAdapter
        comicsList.addOnScrollListener(createOnScrollListener())
        comicsList.setNestedScrollingEnabled(false)

        comicListProgress.visibility = View.GONE

        setInformationViewStatus()
    }

    override fun onBindMoreComics(comics: List<ComicItem>) {
        listComicsAdapter?.onAddMoreItems(comics)

        comicListProgress.visibility = View.GONE

        setInformationViewStatus()
    }

    override fun onBindItem(comic: ComicItem) {
        val position = listComicsAdapter?.indexOf(comic) ?: return

        listComicsAdapter?.updateItem(position, comic, null)
        listComicsAdapter?.notifyItemChanged(position)
    }

    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : EndlessRecyclerView(layoutManager) {

            override fun onLoadMore(current_page: Int) {
                if (presenter.hasMoreComics()) {
                    comicListProgress.visibility = View.VISIBLE

                    when (listComicsModel.listType) {
                        FETCH_ALL -> presenter.onGetMoreComics("All")
                        FETCH_BY_PUBLISHERS, FETCH_BY_SCANLATORS, FETCH_BY_GENRES -> presenter.onGetMoreComics(listComicsModel.link)
                    }

                }
            }
        }
    }

    override fun addOrRemoveFromFavorite(comic: ComicViewModel) {
        presenter.addOrRemoveFromFavorite(comic)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val comic = listComicsAdapter?.getItem(position) ?: return false
        startActivity(ComicDetailsActivity.newIntent(context, comic.comic.pathLink!!, comic.comic.source?.id!!))

        return true
    }

    fun setInformationViewStatus() {
        val adapter = listComicsAdapter ?: return

        if (adapter.isEmpty) {
            listEmptyView.show(R.drawable.ic_comic_laucher_grey_128dp, R.string.no_comics)
        } else {
            listEmptyView.hide()
        }

    }
}