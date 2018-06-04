package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable

const val ONGOING = "ongoing_status"
const val COMPLETED = "completed_status"
const val CANCELED = "canceled_status"
const val UNKNOWN = "unknown_status"

class Comic() : Parcelable {
    var title: String? = ""
    var posterPath: String?= ""
    var status: String?= ""
    var publisher: List<SimpleItem>? = ArrayList()
    var genres: List<SimpleItem>? = ArrayList()
    var authors: List<SimpleItem>? = ArrayList()
    var chapters: List<ChapterItem>? = ArrayList()
    var summary: String?= ""
    var publicationDate: String?= ""
    var scanlators: List<SimpleItem>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        title = parcel.readString()
        posterPath = parcel.readString()
        status = parcel.readString()
        summary = parcel.readString()
        publicationDate = parcel.readString()
    }

    constructor(title: String?, posterPath: String?, status: String?, publisher: List<SimpleItem>?, genres: List<SimpleItem>?, authors: List<SimpleItem>?, chapters: List<ChapterItem>?, summary: String?, publicationDate: String?, scanlators: List<SimpleItem>?) : this() {
        this.title = title
        this.posterPath = posterPath
        this.status = getStatus(status)
        this.publisher = publisher
        this.genres = genres
        this.authors = authors
        this.chapters = chapters
        this.summary = summary
        this.publicationDate = publicationDate
        this.scanlators = scanlators
    }

    private fun getStatus(status: String?): String? {

        if (status != null) {
            if (status.contains("Concluído") or status.contains("Completed")) {
                return COMPLETED
            } else if (status.contains("Em Andamento") or status.contains("Ongoing")) {
                return ONGOING
            } else if (status.contains("Cancelado") or status.contains("Canceled")) {
                return CANCELED
            }
        }

        return UNKNOWN
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(posterPath)
        parcel.writeString(status)
        parcel.writeString(summary)
        parcel.writeString(publicationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Comic> {
        override fun createFromParcel(parcel: Parcel): Comic {
            return Comic(parcel)
        }

        override fun newArray(size: Int): Array<Comic?> {
            return arrayOfNulls(size)
        }
    }


}