package com.tiagohs.hqr.ui.adapters.publishers

import android.view.View
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.ui.adapters.config.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.item_publisher.view.*

class PublisherHolder(
        private val view: View,
        private val adapter: PublishersAdapter
): BaseFlexibleViewHolder(view, adapter) {

    fun bind(item: PublisherItem) {
        view.publisherTitle.text = item.publisher.name

        val backgroundDrawable = ScreenUtils.generateMaterialColorBackground(view.context, item.publisher.name ?: "", view.publisherImgBack)
        view.publisherImgBack.setImageDrawable(backgroundDrawable)
    }
}