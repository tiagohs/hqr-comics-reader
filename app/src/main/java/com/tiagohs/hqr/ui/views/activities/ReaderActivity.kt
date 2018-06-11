package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.viewModels.ReaderModel
import com.tiagohs.hqr.ui.adapters.ReaderPagerAdapter
import com.tiagohs.hqr.ui.callbacks.ISimpleAnimationListener
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_reader.*
import me.zhanghai.android.systemuihelper.SystemUiHelper
import me.zhanghai.android.systemuihelper.SystemUiHelper.*
import javax.inject.Inject


class ReaderActivity: BaseActivity(), ReaderContract.IReaderView {

    companion object {

        const val CHAPTER_MODEL = "CHAPTER_MODEL"

        const val LEFT_REGION = 0.33f
        const val RIGHT_REGION = 0.66f

        fun newIntent(context: Context?, chapterModel: ReaderModel): Intent {
            val intent: Intent = Intent(context, ReaderActivity::class.java)

            intent.putExtra(CHAPTER_MODEL, chapterModel)

            return intent
        }

    }

    @Inject lateinit var presenter: ReaderContract.IReaderPresenter

    lateinit var gestureDetector: GestureDetector

    lateinit var chapter: Chapter
    lateinit var readerModel: ReaderModel

    private var menuVisible = false
    private var systemUi: SystemUiHelper? = null

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

        readerModel = intent.getParcelableExtra(CHAPTER_MODEL)

        setScreenTitle(readerModel.comic.title)
        setScreenSubtitle(readerModel.chapter.title)

        presenter.onBindView(this)

        presenter.onGetChapterDetails(readerModel.pathComic)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    override fun onBindChapter(ch: Chapter?) {
        gestureDetector = GestureDetector(this, ImageGestureListener())

        if (ch != null) {
            chapter = ch
            readerViewPager.adapter = ReaderPagerAdapter(chapter.pages, this, gestureDetector)
            readerViewPager.setCurrentItem(0)

            configurePagesOnSpinner()
            configureNavigationButton()
        }
    }

    private fun configurePagesOnSpinner() {
        pagesSpinner.setItems(chapter.pages.mapIndexed { index, s -> index + 1 })
        pagesSpinner.setOnItemSelectedListener({
            view, position, id, page -> readerViewPager.setCurrentItem(position)
        })
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

            movie_detail_app_bar.visibility = View.VISIBLE

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
                toolbarAnimation.setAnimationListener(object : ISimpleAnimationListener() {
                    override fun onAnimationStart(animation: Animation) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
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
                        movie_detail_app_bar.visibility = View.GONE
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
        }
    }

    protected fun moveToPrevious() {
        if (readerViewPager.currentItem != 0) {
            readerViewPager.setCurrentItem(readerViewPager.currentItem - 1)
        }
    }

    inner class ImageGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val positionX = e.x

            if (positionX < readerViewPager.width * LEFT_REGION) {
                moveLeft()
            } else if (positionX > readerViewPager.width * RIGHT_REGION) {
                moveRight()
            } else {
                toggleMenu()
            }

            return true
        }
    }
}