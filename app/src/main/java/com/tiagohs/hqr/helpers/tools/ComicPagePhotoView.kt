package com.tiagohs.hqr.helpers.tools

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoViewAttacher


class ComicPagePhotoView(imageView: ImageView) : PhotoViewAttacher(imageView), View.OnLongClickListener, View.OnTouchListener {

    var gestureDetector: GestureDetector? = null

    override fun onLongClick(v: View): Boolean {
        return false
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        return super.onTouch(view, event)
    }

}