package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.chrisbanes.photoview.OnViewTapListener
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.ImageUtils
import com.tiagohs.hqr.models.sources.Page
import kotlinx.android.synthetic.main.item_reader_image.view.*




class ReaderPagerAdapter(
        private val imagesPaths: List<Page>,
        private val context: Context?,
        private val onPageTapListener: OnViewTapListener
): PagerAdapter() {



    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.item_reader_image, container, false)
        val image = imagesPaths.get(position)

        ImageUtils.load(view.chapterImg,image.imageUrl)

        val attacher = PhotoViewAttacher(view.chapterImg)
        attacher.setOnViewTapListener (onPageTapListener)
        attacher.update();

        container.addView(view)

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return imagesPaths.size
    }

}