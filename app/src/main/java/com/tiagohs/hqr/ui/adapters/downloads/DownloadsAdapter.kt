package com.tiagohs.hqr.ui.adapters.downloads

import com.tiagohs.hqr.ui.callbacks.IDownloadCallback
import eu.davidea.flexibleadapter.FlexibleAdapter

class DownloadsAdapter(
    listener: IDownloadCallback
): FlexibleAdapter<DownloadItem>(null, listener, true) {
}