package com.tiagohs.hqr.models.sources

import android.os.Parcel
import android.os.Parcelable

class ReaderModel(
        val pathComic: String,
        val comicTitle: String?,
        val comicChapterTitle: String?
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pathComic)
        parcel.writeString(comicTitle)
        parcel.writeString(comicChapterTitle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReaderModel> {
        override fun createFromParcel(parcel: Parcel): ReaderModel {
            return ReaderModel(parcel)
        }

        override fun newArray(size: Int): Array<ReaderModel?> {
            return arrayOfNulls(size)
        }
    }

}