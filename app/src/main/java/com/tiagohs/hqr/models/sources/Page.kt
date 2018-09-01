package com.tiagohs.hqr.models.sources

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.helpers.tools.ProgressListener
import com.tiagohs.hqr.models.database.comics.Chapter
import io.reactivex.subjects.PublishSubject

class Page(
        var index: Int,
        var url: String = "",
        var imageUrl: String? = null,
        var uri: Uri? = null,
        var isAd: Boolean = false
): ProgressListener, Parcelable {

    companion object CREATOR : Parcelable.Creator<Page> {

        override fun createFromParcel(parcel: Parcel): Page {
            return Page(parcel)
        }

        override fun newArray(size: Int): Array<Page?> {
            return arrayOfNulls(size)
        }

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

    @Transient private var statusSubject: PublishSubject<String>? = null

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable(Uri::class.java.classLoader)) {
    }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    fun setStatusSubject(subject: PublishSubject<String>?) {
        this.statusSubject = subject
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
        parcel.writeString(url)
        parcel.writeString(imageUrl)
        parcel.writeParcelable(uri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

}