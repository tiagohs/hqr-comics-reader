package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.models.view_models.FETCH_BY_PUBLISHERS
import com.tiagohs.hqr.models.view_models.ListComicsModel
import com.tiagohs.hqr.ui.adapters.ComicDetailsPagerAdapter
import com.tiagohs.hqr.ui.adapters.SimpleItemAdapter
import com.tiagohs.hqr.ui.callbacks.ISimpleItemCallback
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_comic_details.*
import javax.inject.Inject

private const val COMIC_LINK = "comic_link"

class ComicDetailsActivity: BaseActivity(), ComicDetailsContract.IComicDetailsView {

    companion object {

        fun newIntent(context: Context?, comicLink: String): Intent {
            val intent: Intent = Intent(context, ComicDetailsActivity::class.java)

            intent.putExtra(COMIC_LINK, comicLink)

            return intent
        }
    }

    @Inject
    lateinit var presenter: ComicDetailsContract.IComicDetailsPresenter

    override fun onGetMenuLayoutId(): Int = 0

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_comic_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        presenter.onBindView(this)

        val comicLink = intent.getStringExtra(COMIC_LINK) ?: ""

        if (comicLink.isNotEmpty()) {
            presenter.onGetComicData(comicLink)
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onUnbindView()
    }

    override fun onBindComic(comic: ComicViewModel) {
        comicsDetailsViewpager.adapter = ComicDetailsPagerAdapter(supportFragmentManager, mutableListOf("Resume", "Chapters"), comic)
        tabLayout.setupWithViewPager(comicsDetailsViewpager)

        if (!comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(comicImage,
                    "https://hqbr.com.br/" + comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    false)
            ImageUtils.loadWithRevealAnimation(this, comicWallpaper,
                    "https://hqbr.com.br/" + comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder)
        }

        comicTitle.text = comic.name

        comicStatus.text = ScreenUtils.getComicStatusText(this, comic.status)
        comicStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(this, comic.status))

        publishersList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        publishersList.adapter = SimpleItemAdapter(comic.publisher, this, onPublisherSelect())
    }

    fun onPublisherSelect(): ISimpleItemCallback {
        return object : ISimpleItemCallback {
            override fun onClick(item: DefaultModelView) {
                startActivity(ListComicsActivity.newIntent(this@ComicDetailsActivity, ListComicsModel(FETCH_BY_PUBLISHERS, item.name!!, item.pathLink!!)))
            }
        }
    }

}