package com.tiagohs.hqr.models

import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.models.sources.Source
import io.reactivex.processors.PublishProcessor

class Download(
        val source: Source,
        val comic: Comic,
        val chapter: Chapter
) {

    companion object {
        const val NOT_DOWNLOADED = "NOT_DOWNLOADED"
        const val QUEUE = "QUEUE"
        const val DOWNLOADING = "DOWNLOADING"
        const val DOWNLOADED = "DOWNLOADED"
        const val ERROR = "ERROR"
    }

    private var statusSubject: PublishProcessor<Download>? = null

    var progressTotal: Int = 0
    var numberOfImagesDownloaded: Int = 0

    var status: String = NOT_DOWNLOADED
        set(value) {
            field = value
            statusSubject?.onNext(this)
        }

    fun setStatusSubject(statusSubject: PublishProcessor<Download>?) {
        this.statusSubject = statusSubject
    }
}