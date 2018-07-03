package com.tiagohs.hqr.ui.adapters.downloads_queue

import android.content.Context
import com.tiagohs.hqr.ui.callbacks.IDownloadQueuerListener
import eu.davidea.flexibleadapter.FlexibleAdapter

class DownloadQueueAdapter(
        listener: IDownloadQueuerListener
): FlexibleAdapter<DownloadQueueItem>(null, listener, true) {

    var items: List<DownloadQueueItem> = emptyList()

    override fun updateDataSet(items: List<DownloadQueueItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

}