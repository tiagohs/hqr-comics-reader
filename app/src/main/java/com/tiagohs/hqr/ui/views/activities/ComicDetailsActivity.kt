package com.tiagohs.hqr.ui.views.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.AnimationUtils
import com.squareup.picasso.Callback
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.tools.AppBarMovieListener
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.helpers.utils.PermissionUtils
import com.tiagohs.hqr.helpers.utils.PermissionsCallback
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.view_models.*
import com.tiagohs.hqr.ui.adapters.SimpleItemAdapter
import com.tiagohs.hqr.ui.adapters.pagers.ComicDetailsPagerAdapter
import com.tiagohs.hqr.ui.callbacks.ISimpleItemCallback
import com.tiagohs.hqr.ui.contracts.ComicDetailsContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_comic_details.*
import java.lang.Exception
import javax.inject.Inject


class ComicDetailsActivity: BaseActivity(), ComicDetailsContract.IComicDetailsView, PermissionsCallback {

    companion object {
        const val COMIC_LINK = "comic_link"

        fun newIntent(context: Context?, comicLink: String): Intent {
            val intent: Intent = Intent(context, ComicDetailsActivity::class.java)

            intent.putExtra(COMIC_LINK, comicLink)

            return intent
        }
    }

    @Inject
    lateinit var presenter: ComicDetailsContract.IComicDetailsPresenter

    val permissions: PermissionUtils = PermissionUtils(this)

    var comic: ComicViewModel? = null

    override fun onGetMenuLayoutId(): Int = 0

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_comic_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        permissions.onCheckAndRequestPermissions(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), this)

        presenter.onBindView(this)

        val comicLink = intent.getStringExtra(COMIC_LINK) ?: ""

        if (comicLink.isNotEmpty()) {
            presenter.onGetComicData(comicLink)
        }
    }

    private fun onOffsetChangedListener(): AppBarMovieListener {
        return object : AppBarMovieListener() {
            override fun onExpanded(appBarLayout: AppBarLayout) {
                setScreenTitle("")
                readBtn.visibility = View.VISIBLE
            }

            override fun onCollapsed(appBarLayout: AppBarLayout) {
                setScreenTitle(comic?.name ?: "")
                readBtn.visibility = View.GONE
            }

            override fun onIdle(appBarLayout: AppBarLayout) {
                setScreenTitle("")
                readBtn.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onUnbindView()
    }

    override fun onBindComic(comic: ComicViewModel, history: ComicHistoryViewModel?) {
        this.comic = comic

        onConfigureTabs()
        onConfigureAppBar()

        if (!comic.posterPath.isNullOrEmpty()) {
            ImageUtils.load(comicImage,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder,
                    false)
            comicWallpaper.contentDescription = getString(R.string.comic_details_poster_custom_description, comic.name)

            com.tiagohs.hqr.helpers.utils.AnimationUtils.creatScaleUpAnimation(comicImage, 500)

            ImageUtils.loadWithRevealAnimation(this, comicWallpaper,
                    comic.posterPath,
                    R.drawable.img_placeholder,
                    R.drawable.img_placeholder, object: Callback {
                            override fun onSuccess() {
                                comicWallpaperOverlay.visibility = View.VISIBLE
                            }

                            override fun onError(e: Exception?) {
                                comicWallpaperOverlay.visibility = View.VISIBLE
                            }
            })
            comicWallpaper.contentDescription = getString(R.string.comic_details_wallpaper_custom_description, comic.name)
        }

        comicTitle.text = comic.name

        comicStatus.text = ScreenUtils.getComicStatusText(this, comic.status)
        comicStatus.setBackgroundColor(ScreenUtils.generateComicStatusBackground(this, comic.status))

        publishersList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        publishersList.adapter = SimpleItemAdapter(comic.publisher, this, onPublisherSelect())

        // VISIBLES
        headerContainer.visibility = View.VISIBLE
        comicDetailsTabContainer.visibility = View.VISIBLE
        comicDetailsProgress.visibility = View.GONE

        readBtn.visibility = View.VISIBLE
        readBtn.startAnimation(AnimationUtils.loadAnimation(this, com.github.clans.fab.R.anim.fab_scale_up))

        readBtn.text = if (history != null)
            "Continuar"
        else
            "Ler"

        readBtn.setOnClickListener {

            if (history != null) {
                startActivity(ReaderActivity.newIntent(this, history.chapter?.chapterPath!!, comic.pathLink!!))
            } else {
                val chapter = comic.chapters?.last()

                if (chapter != null) {
                    startActivity(ReaderActivity.newIntent(this, chapter.chapterPath!!, comic.pathLink!!))
                }
            }
        }

        onConfigureFavoriteBtn(comic)
    }

    override fun onConfigureFavoriteBtn(comic: ComicViewModel) {
        addToFavBtn.visibility = View.VISIBLE
        com.tiagohs.hqr.helpers.utils.AnimationUtils.createScaleButtonAnimation(addToFavBtn)

        addToFavBtn.isChecked = comic.favorite
        addToFavBtn.setOnClickListener({ presenter.addOrRemoveFromFavorite(comic) })
    }

    private fun onConfigureAppBar() {
        comicDetailsAppBar.addOnOffsetChangedListener(onOffsetChangedListener())
    }

    private fun onConfigureTabs() {
        tabLayout.visibility = View.VISIBLE

        comicsDetailsViewpager.adapter = ComicDetailsPagerAdapter(supportFragmentManager, resources.getStringArray(R.array.comic_details_tabs_values).toList(), comic!!)
        tabLayout.setupWithViewPager(comicsDetailsViewpager)
    }

    fun onPublisherSelect(): ISimpleItemCallback {
        return object : ISimpleItemCallback {
            override fun onClick(item: DefaultModelView) {
                startActivity(ListComicsActivity.newIntent(this@ComicDetailsActivity, ListComicsModel(FETCH_BY_PUBLISHERS, item.name!!, item.pathLink!!)))
            }
        }
    }

    override fun onPermissionsGranted() {

    }

    override fun onPermissionsDenied() {

    }

    override fun onNeverAskAgain(requestCode: Int) {

    }


}