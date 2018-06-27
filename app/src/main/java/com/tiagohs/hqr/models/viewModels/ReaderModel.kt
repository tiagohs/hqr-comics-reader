package com.tiagohs.hqr.models.viewModels

import android.os.Parcel
import android.os.Parcelable

class ReaderModel(
        val pathComic: String,
        val comic: ComicViewModel,
        val chapter: ChapterViewModel
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readParcelable(ComicViewModel::class.java.classLoader),
            parcel.readParcelable(ChapterViewModel::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pathComic)
        parcel.writeParcelable(comic, flags)
        parcel.writeParcelable(chapter, flags)
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