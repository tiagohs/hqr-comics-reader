package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable

class ChapterItem(
        val title: String?,
        val link: String?,
        val comicTitle: String?): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(link)
        parcel.writeString(comicTitle)
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