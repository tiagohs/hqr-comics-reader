package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.github.chrisbanes.photoview.OnViewTapListener
import com.google.android.gms.ads.AdRequest
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ReaderChapterViewModel
import com.tiagohs.hqr.ui.adapters.ReaderPagerAdapter
import com.tiagohs.hqr.ui.callbacks.IOnTouch
import com.tiagohs.hqr.ui.callbacks.ISimpleAnimationListener
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_reader.*
import me.zhanghai.android.systemuihelper.SystemUiHelper
import me.zhanghai.android.systemuihelper.SystemUiHelper.*
import javax.inject.Inject


class ReaderActivity: BaseActivity(), ReaderContract.IReaderView, IOnTouch {

    companion object {

        const val COMIC_PATH = "CHAPTER_MODEL"
        const val CHAPTER_PATH = "CHAPTER_PATH"

        const val LEFT_REGION = 0.33f
        const val RIGHT_REGION = 0.66f

        fun newIntent(context: Context?, chapterPath: String, comicPath: String): Intent {
            val intent: Intent = Intent(context, ReaderActivity::class.java)

            intent.putExtra(COMIC_PATH, comicPath)
            intent.putExtra(CHAPTER_PATH, chapterPath)

            return intent
        }

    }

    @Inject lateinit var presenter: ReaderContract.IReaderPresenter

    private var menuVisible = false
    private var systemUi: SystemUiHelper? = null

    private var readerChapterViewModel: ReaderChapterViewModel? = null

    private var adapter: ReaderPagerAdapter? = null

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_reader
    }

    override fun onGetMenuLayoutId(): Int {
        return 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        setMenuVisibility(menuVisible)
        setFullscreen(true)

        presenter.onBindView(this)
        presenter.onCreate()

        adView.loadAd(AdRequest.Builder().build())

        onInit()
    }

    private fun onInit() {
        val comicPath = intent.getStringExtra(COMIC_PATH)
        val chapterPath = intent.getStringExtra(CHAPTER_PATH)

        presenter.onGetChapterDetails(comicPath, chapterPath)
    }

    override fun onError(ex: Throwable, message: Int, withAction: Boolean) {
        readerPageProgress.visibility = View.GONE

        super.onError(ex, message, withAction)
    }

    override fun onErrorAction() {
        readerPageProgress.visibility = View.VISIBLE

        onInit()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onUnbindView()
    }

    override fun onPause() {
        super.onPause()

        presenter.onSaveUserHistory()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    override fun onBindChapter(model: ReaderChapterViewModel, updateDataSet: Boolean) {
        this.readerChapterViewModel = model

        readerPageProgress.visibility = View.GONE

        setScreenTitle(model.comic.name)
        setScreenSubtitle(model.chapter.chapterName)

        adapter = ReaderPagerAdapter(model,  this, onPageTapListener())
        readerViewPager.adapter = adapter

        if (model.chapter.lastPageRead != -1 && model.chapter.lastPageRead < model.pages.size) {
            readerViewPager.setCurrentItem(model.chapter.lastPageRead)

            if (model.chapter.lastPageRead == 0) {
                presenter.onTrackUserHistory(model.pages.first())
            }
        } else {
            readerViewPager.setCurrentItem(0)
            presenter.onTrackUserHistory(model.chapter.pages?.get(0))
        }

        readerViewPager.addOnPageChangeListener(onConfigureViewPageListener())
        readerViewPager.listener = this

        configurePagesOnSpinner()
        configureNavigationButton()
    }

    override fun onTouchPageView(ev: MotionEvent) {

        if (ev.getAction() == MotionEvent.ACTION_MOVE &&
            ev.x < readerViewPager.width * LEFT_REGION &&
            readerViewPager.currentItem == readerViewPager.adapter!!.count - 1) {

            onRequestNextChapter()
        }

    }

    private fun onConfigureViewPageListener(): ViewPager.OnPageChangeListener {
        return object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageSelected(position: Int) {
                pagesSpinner.selectedIndex = position

                val page = adapter?.pages?.getOrNull(position) ?: return

                if (!page.isAd) {
                    presenter.onTrackUserHistory(page)
                }
            }
        }
    }

    override fun onPageDownloaded(page: Page) {
        adapter?.updatePage(page)
    }

    fun onRequestNextChapter() {
        adapter?.pages = emptyList()
        adapter?.notifyDataSetChanged()

        readerPageProgress.visibility = View.VISIBLE

        presenter.onRequestNextChapter()
    }

    private fun onPageTapListener(): OnViewTapListener {
        return object: OnViewTapListener {
            override fun onViewTap(view: View?, x: Float, y: Float) {
                val positionX = x

                if (positionX < readerViewPager.width * LEFT_REGION) {
                    moveLeft()
                } else if (positionX > readerViewPager.width * RIGHT_REGION) {
                    moveRight()
                } else {
                    toggleMenu()
                }
            }
        }
    }

    private fun configurePagesOnSpinner() {
        val indexs = readerChapterViewModel?.pages?.mapIndexed { index, s -> index + 1 } ?: return

        pagesSpinner.setItems(indexs)
        pagesSpinner.setOnItemSelectedListener({
            view, position, id, p -> readerViewPager.setCurrentItem(position)
        })
        pagesSpinner.selectedIndex = 0
    }

    private fun configureNavigationButton() {
        rightButton.setOnClickListener { moveRight() }
        leftButton.setOnClickListener { moveLeft() }
    }

    private fun setFullscreen(enabled: Boolean) {
        systemUi = if (enabled) {
            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) LEVEL_IMMERSIVE else LEVEL_HIDE_STATUS_BAR
            val flags = FLAG_IMMERSIVE_STICKY or FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES
            SystemUiHelper(this, level, flags)
        } else {
            null
        }
    }

    protected fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    protected fun moveRight() {
        moveToNext()
    }

    protected fun moveLeft() {
        moveToPrevious()
    }

    private fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        menuVisible = visible

        if (visible) {
            systemUi?.show()

            toolbar.visibility = View.VISIBLE
            readerBottomMenu.visibility = View.VISIBLE

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
                toolbarAnimation.setAnimationListener(object : ISimpleAnimationListener() {
                    override fun onAnimationStart(animation: Animation) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                            window.setStatusBarColor(ContextCompat.getColor(this@ReaderActivity , android.R.color.black));
                        }
                    }
                })

                toolbar.startAnimation(toolbarAnimation)

                val bottomMenuAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
                readerBottomMenu.startAnimation(bottomMenuAnimation)
            }
        } else {
            systemUi?.hide()

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_top)
                toolbarAnimation.setAnimationListener(object : ISimpleAnimationListener() {
                    override fun onAnimationEnd(animation: Animation) {
                        toolbar.visibility = View.GONE
                        readerBottomMenu.visibility = View.GONE
                    }
                })

                toolbar.startAnimation(toolbarAnimation)


                val bottomMenuAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_bottom)
                readerBottomMenu.startAnimation(bottomMenuAnimation)
            }
        }
    }

    protected fun moveToNext() {
        if (readerViewPager.currentItem != readerViewPager.adapter!!.count - 1) {
            readerViewPager.setCurrentItem(readerViewPager.currentItem + 1)
        } else {
            onRequestNextChapter()
        }
    }

    protected fun moveToPrevious() {
        if (readerViewPager.currentItem != 0) {
            readerViewPager.setCurrentItem(readerViewPager.currentItem - 1)
        }
    }

}