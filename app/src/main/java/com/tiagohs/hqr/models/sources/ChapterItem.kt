package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.R.id.comicTitle

class ChapterItem(): Parcelable {
    var title: String? = ""
    var link: String? = ""
    var comicTitle: String? = ""
    var source_order: Int? = 0

    constructor(parcel: Parcel) : this() {
        title = parcel.readString()
        link = parcel.readString()
        comicTitle = parcel.readString()
        source_order = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(link)
        parcel.writeString(comicTitle)
        parcel.writeValue(source_order)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChapterItem> {
        override fun createFromParcel(parcel: Parcel): ChapterItem {
            return ChapterItem(parcel)
        }

        override fun newArray(size: Int): Array<ChapterItem?> {
            return arrayOfNulls(size)
        }
    }

}