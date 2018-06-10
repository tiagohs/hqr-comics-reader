package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.viewModels.ReaderModel
import com.tiagohs.hqr.ui.adapters.ReaderPagerAdapter
import com.tiagohs.hqr.ui.contracts.ReaderContract
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_reader.*
import javax.inject.Inject

private const val CHAPTER_MODEL = "CHAPTER_MODEL"

class ReaderActivity: BaseActivity(), ReaderContract.IReaderView {

    companion object {

        fun newIntent(context: Context?, chapterModel: ReaderModel): Intent {
            val intent: Intent = Intent(context, ReaderActivity::class.java)

            intent.putExtra(CHAPTER_MODEL, chapterModel)

            return intent
        }
    }


    @Inject lateinit var presenter: ReaderContract.IReaderPresenter

    lateinit var chapter: Chapter
    lateinit var readerModel: ReaderModel

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_reader
    }

    override fun onGetMenuLayoutId(): Int {
        return 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()!!.inject(this)

        readerModel = intent.getParcelableExtra(CHAPTER_MODEL)

        presenter.onBindView(this)

        presenter.onGetChapterDetails(readerModel.pathComic)
    }

    override fun onBindChapter(ch: Chapter?) {
        if (ch != null) {
            chapter = ch
            readerViewPager.adapter = ReaderPagerAdapter(chapter.pages, this)
            readerViewPager.setCurrentItem(0)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            readerViewPager.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


}