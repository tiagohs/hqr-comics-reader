package com.tiagohs.hqr.ui.adapters.publishers

import com.tiagohs.hqr.ui.callbacks.IPublisherCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class PublishersAdapter(
        private val listener: IPublisherCallback) : FlexibleAdapter<PublisherItem>(null, listener, true) {

    var items: List<PublisherItem> = emptyList()

    override fun updateDataSet(items: List<PublisherItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun onAddMoreItems(items: List<PublisherItem>?) {
        updateDataSet(items)

        notifyDataSetChanged()
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }
}