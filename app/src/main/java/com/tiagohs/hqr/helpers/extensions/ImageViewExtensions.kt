package com.tiagohs.hqr.helpers.extensions

import android.support.annotation.DrawableRes
import android.support.graphics.drawable.VectorDrawableCompat
import android.widget.ImageView


fun ImageView.setVectorCompat(@DrawableRes drawable: Int, tint: Int? = null) {
    val vector = VectorDrawableCompat.create(resources, drawable, context.theme)
    if (tint != null) {
        vector?.mutate()
        vector?.setTint(tint)
    }
    setImageDrawable(vector)
}