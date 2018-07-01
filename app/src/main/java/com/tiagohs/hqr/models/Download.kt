package com.tiagohs.hqr.models

import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.IHttpSource
import io.reactivex.processors.PublishProcessor

class Download(
        val source: IHttpSource,
        val sourceDB: SourceDB,
        val comic: ComicViewModel,
        val chapter: ChapterViewModel
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