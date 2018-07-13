package com.tiagohs.hqr.helpers.tools

import android.content.Context
import android.util.AttributeSet
import com.github.chrisbanes.photoview.OnViewTapListener
import com.github.chrisbanes.photoview.PhotoView

class PageViewer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : PhotoView(context, attrs) {

    private var tapListener: OnViewTapListener? = null

    fun setCustomOnViewTapListener(tapListener: OnViewTapListener) {
        this.tapListener = tapListener
    }

}