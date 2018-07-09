package com.tiagohs.hqr.ui.adapters.config

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder

abstract class BaseFlexibleViewHolder(
        private val view: View,
        private val adapter: FlexibleAdapter<*>
): FlexibleViewHolder(view, adapter) {

    val containerViewHolder: View = itemView
}