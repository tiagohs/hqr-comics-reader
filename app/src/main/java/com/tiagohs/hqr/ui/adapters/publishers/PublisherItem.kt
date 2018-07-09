package com.tiagohs.hqr.ui.adapters.publishers

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.DefaultModelView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

class PublisherItem(
        val publisher: DefaultModelView
): AbstractFlexibleItem<PublisherHolder>() {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: PublisherHolder, position: Int, payloads: MutableList<Any>?) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is PublisherItem) {
            return publisher.id == other.publisher.id
        }
        return false
    }

    override fun hashCode(): Int = publisher.id.hashCode()

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): PublisherHolder {
        return PublisherHolder(view, adapter as PublishersAdapter)
    }

    override fun getLayoutRes(): Int = R.layout.item_publisher
}