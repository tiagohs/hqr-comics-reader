package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable

class Chapter(): Parcelable {
    var id: String? = ""
    var comicId: String? = ""
    var chapterPath: String? = ""
    var pages: List<Page>? = ArrayList()
    var name: String? = ""
    var sourceOrder: Int? = 0

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        comicId = parcel.readString()
        chapterPath = parcel.readString()
        pages = parcel.createTypedArrayList(Page)
        name = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(comicId)
        parcel.writeString(chapterPath)
        parcel.writeTypedList(pages)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chapter> {
        override fun createFromParcel(parcel: Parcel): Chapter {
            return Chapter(parcel)
        }

        override fun newArray(size: Int): Array<Chapter?> {
            return arrayOfNulls(size)
        }
    }


}