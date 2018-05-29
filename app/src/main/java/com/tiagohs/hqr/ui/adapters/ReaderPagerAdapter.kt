package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.utils.ImageUtils
import kotlinx.android.synthetic.main.item_reader_image.view.*

class ReaderPagerAdapter(
        private val imagesPaths: List<String>,
        private val context: Context?
): PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.item_reader_image, container, false)
        val image = imagesPaths.get(position)

        ImageUtils.load(view.chapterImg,"https://hqbr.com.br/" + image)

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