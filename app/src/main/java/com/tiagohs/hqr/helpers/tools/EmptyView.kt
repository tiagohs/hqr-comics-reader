package com.tiagohs.hqr.helpers.tools

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.getResourceColor
import com.tiagohs.hqr.helpers.extensions.setVectorCompat
import kotlinx.android.synthetic.main.empty_view_default.view.*

class EmptyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : RelativeLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.empty_view_default, this)
    }

    fun setCustomView(layout: Int) {
        View.inflate(context, layout, this)
    }

    fun hide() {
        this.visibility = View.GONE
    }

    fun show(drawable: Int, textInfo: Int) {
        emptyImageView.setVectorCompat(drawable, context.getResourceColor(R.color.colorSecondaryText))
        emptyLabel.text = context.getString(textInfo)

        visibility = View.VISIBLE
    }

    fun show(textInfo: Int) {
        emptyLabel.text = context.getString(textInfo)

        visibility = View.VISIBLE
    }
}