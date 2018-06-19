package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.helpers.utils.ScreenUtils

const val ONGOING = "ongoing_status"
const val COMPLETED = "completed_status"
const val CANCELED = "canceled_status"
const val UNKNOWN = "unknown_status"

class Comic() : Parcelable {
    var id: String? = ""
    var pathLink: String? = ""
    var title: String? = ""
    var posterPath: String?= ""
    var sourceId: Long = 0L

    var publisher: List<SimpleItem>? = ArrayList()
    var genres: List<SimpleItem>? = ArrayList()
    var authors: List<SimpleItem>? = ArrayList()
    var chapters: List<Chapter>? = ArrayList()
    var summary: String?= ""
    var publicationDate: String?= ""
    var scanlators: List<SimpleItem>? = ArrayList()

    var status: String? = ""
        set(value) {
            field = ScreenUtils.getStatusConstant(value)
        }

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        pathLink = parcel.readString()
        title = parcel.readString()
        posterPath = parcel.readString()
        sourceId = parcel.readLong()
        chapters = parcel.createTypedArrayList(Chapter)
        summary = parcel.readString()
        publicationDate = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(pathLink)
        parcel.writeString(title)
        parcel.writeString(posterPath)
        parcel.writeLong(sourceId)
        parcel.writeTypedList(chapters)
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