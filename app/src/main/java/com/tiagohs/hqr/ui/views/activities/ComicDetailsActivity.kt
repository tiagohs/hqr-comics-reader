package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.ui.adapters.ComicDetailsPagerAdapter
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

    override fun onGetMenuLayoutId(): Int {
        return R.menu.menu_comic_details
    }

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

        comicsDetailsViewpager.adapter = ComicDetailsPagerAdapter(supportFragmentManager, mutableListOf("Resume", "Chapters"))
        tabLayout.setupWithViewPager(comicsDetailsViewpager)
    }

    override fun onBindComic(comic: Comic) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAdded(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}