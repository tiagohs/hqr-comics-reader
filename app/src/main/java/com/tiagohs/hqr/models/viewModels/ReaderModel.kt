package com.tiagohs.hqr.models.viewModels

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.ChapterItem
import com.tiagohs.hqr.models.sources.Comic

class ReaderModel(
        val pathComic: String,
        val comic: Comic,
        val chapter: Chapter
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readParcelable(Comic::class.java.classLoader),
            parcel.readParcelable(ChapterItem::class.java.classLoader)) {
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