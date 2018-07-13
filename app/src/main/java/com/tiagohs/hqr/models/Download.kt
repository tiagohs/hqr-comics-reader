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
        const val NOT_DOWNLOADED = 0
        const val QUEUE = 1
        const val DOWNLOADING = 2
        const val DOWNLOADED = 3
        const val ERROR = 4
    }

    private var statusSubject: PublishProcessor<Download>? = null

    var progressTotal: Int = 0
    var numberOfImagesDownloaded: Int = 0

    var status: Int = NOT_DOWNLOADED
        set(value) {
            field = value
            statusSubject?.onNext(this)
        }

    fun setStatusSubject(statusSubject: PublishProcessor<Download>?) {
        this.statusSubject = statusSubject
    }
}