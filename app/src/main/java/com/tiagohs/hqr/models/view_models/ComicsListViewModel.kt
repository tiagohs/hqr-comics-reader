package com.tiagohs.hqr.models.view_models

import android.os.Parcel
import android.os.Parcelable

class ComicsListViewModel(
        var comics: List<ComicViewModel>,
        var hasPagesSupport: Boolean
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.createTypedArrayList(ComicViewModel),
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(comics)
        parcel.writeByte(if (hasPagesSupport) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ComicsListViewModel> {
        override fun createFromParcel(parcel: Parcel): ComicsListViewModel {
            return ComicsListViewModel(parcel)
        }

        override fun newArray(size: Int): Array<ComicsListViewModel?> {
            return arrayOfNulls(size)
        }
    }

}