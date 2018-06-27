package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable

class ComicsItem(
        val title: String,
        val imagePath: String,
        val link: String,
        val publisher: String,
        val status: String): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(imagePath)
        parcel.writeString(link)
        parcel.writeString(publisher)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ComicsItem> {
        override fun createFromParcel(parcel: Parcel): ComicsItem {
            return ComicsItem(parcel)
        }

        override fun newArray(size: Int): Array<ComicsItem?> {
            return arrayOfNulls(size)
        }
    }
}