package com.tiagohs.hqr.models.view_models

import android.os.Parcel
import android.os.Parcelable

const val FETCH_ALL: String = "FETCH_ALL"
const val FETCH_BY_SCANLATORS: String = "FETCH_BY_SCANLATORS"
const val FETCH_BY_PUBLISHERS: String = "FETCH_BY_PUBLISHERS"

class ListComicsModel(
    val listType: String,
    val pageTitle: String,
    val link: String
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(listType)
        parcel.writeString(pageTitle)
        parcel.writeString(link)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ListComicsModel> {
        override fun createFromParcel(parcel: Parcel): ListComicsModel {
            return ListComicsModel(parcel)
        }

        override fun newArray(size: Int): Array<ListComicsModel?> {
            return arrayOfNulls(size)
        }
    }

}