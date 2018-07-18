package com.tiagohs.hqr.ui.adapters

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.chrisbanes.photoview.OnViewTapListener
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.google.android.gms.ads.AdRequest
import com.squareup.picasso.Callback
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ReaderChapterViewModel
import kotlinx.android.synthetic.main.ad_retangule.view.*
import kotlinx.android.synthetic.main.item_reader_chapter_apresentation.view.*
import kotlinx.android.synthetic.main.item_reader_image.view.*
import java.lang.Exception


class ReaderPagerAdapter(
        var chapterReader: ReaderChapterViewModel,
        val context: Context?,
        val onPageTapListener: OnViewTapListener
): PagerAdapter() {

    var isFirstTime: Boolean = true
    var pages: List<Page> = chapterReader.pages

    fun updatePage(p: Page) {
        val page = pages.find { it.imageUrl.equals(p.imageUrl) }

        page?.status = p.status
        page?.uri = p.uri
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.item_reader_image, container, false)
        val page = pages.get(position)

        if (position == 0 && isFirstTime) {
            onShowComicApresentationView(view, page)
        } else if(page.isAd) {
            onLoadAd(view)
        } else {
            onLoadPage(view, page)
        }

        container.addView(view)

        return view
    }

    private fun onLoadAd(view: View) {
        val adView = view.findViewById<CoordinatorLayout>(R.id.adRetanguleContainer)

        adView.adRetanguleView.loadAd(AdRequest.Builder().build())
        adView.visibility = View.VISIBLE
    }

    private fun onShowComicApresentationView(view: View, page: Page) {
        val apresentationView = view.findViewById<LinearLayout>(R.id.apresentationContainer)

        apresentationView.comicTitle.text = chapterReader.comic.name
        apresentationView.comicChapterTitle.text = chapterReader.chapter.chapterName

        apresentationView.visibility = View.VISIBLE

        onCreateAlphaAnimation(view, apresentationView, page)

        isFirstTime = false
    }

    private fun onCreateAlphaAnimation(view: View, apresentationView: View, page: Page) {
        view.postDelayed(Runnable {
            val animator = ValueAnimator.ofFloat(1f, 0f)

            animator.addUpdateListener { animation ->
                onAnimationAlphaUpdate(apresentationView, animation)
            }
            animator.addListener(object: Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    apresentationView.setVisibility(View.GONE)
                    onLoadPage(view, page)
                }

                override fun onAnimationRepeat(p0: Animator?) {}
                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {}

            })

            animator.duration = 600
            animator.start()
        }, 3000)
    }

    private fun onAnimationAlphaUpdate(apresentationView: View, animation: ValueAnimator) {
        val alpha = animation.animatedValue as Float
        apresentationView.setAlpha(alpha)
    }

    private fun onLoadPage(view: View, page: Page) {

        if (page.status == Page.READY) {
            ImageUtils.load(view.chapterImg, page.uri, object : Callback {
                override fun onSuccess() {
                    view.chapterPageProgress?.visibility = View.GONE

                    val attacher = PhotoViewAttacher(view.chapterImg)
                    attacher.setOnViewTapListener (onPageTapListener)
                    attacher.update()
                }

                override fun onError(e: Exception?) {}
            }, true)
        } else {
            ImageUtils.load(view.chapterImg, page.imageUrl, object : Callback {
                override fun onSuccess() {
                    view.chapterPageProgress?.visibility = View.GONE

                    val attacher = PhotoViewAttacher(view.chapterImg)
                    attacher.setOnViewTapListener (onPageTapListener)
                    attacher.update()
                }

                override fun onError(e: Exception?) {}
            }, true)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return pages.size
    }

}