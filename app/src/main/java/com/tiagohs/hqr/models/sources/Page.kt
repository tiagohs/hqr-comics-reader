package com.tiagohs.hqr.models.sources

import android.net.Uri
import com.tiagohs.hqr.utils.extensions.ProgressListener
import io.reactivex.subjects.Subject

class Page(
        val index: Int,
        val url: String = "",
        var imageUrl: String? = null,
        var uri: Uri? = null
): ProgressListener {

    companion object {
        const val QUEUE = "QUEUE"
        const val LOAD_PAGE = "LOAD_PAGE"
        const val DOWNLOAD_IMAGE = "DOWNLOAD_IMAGE"
        const val READY = "READY"
        const val ERROR = "ERROR"
    }


    val number: Int
        get() = index + 1

    @Transient lateinit var chapter: Chapter

    @Transient @Volatile var status: String = QUEUE
        set(value) {
            field = value
            statusSubject?.onNext(value)
        }

    @Transient @Volatile var progress: Int = 0

    @Transient private var statusSubject: Subject<String>? = null

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    fun setStatusSubject(subject: Subject<String>?) {
        this.statusSubject = subject
    }


}