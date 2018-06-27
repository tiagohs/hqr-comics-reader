package com.tiagohs.hqr.models.viewModels

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.models.base.IChapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.sources.Page

class ChapterViewModel() : Parcelable {

    var id: Long = -1L
    var chapterName: String? = ""
    var chapterPath: String? = ""
    var lastPageRead: Int = 0

    var pages: List<Page>? = null
    var comic: Comic? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        chapterName = parcel.readString()
        chapterPath = parcel.readString()
        lastPageRead = parcel.readInt()
        pages = parcel.createTypedArrayList(Page)
    }

    fun create(other: IChapter): ChapterViewModel {
        return ChapterViewModel().apply {
            copyFrom(other)
        }
    }

    fun copyFrom(other: ChapterViewModel) {
        this.lastPageRead = other.lastPageRead

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.chapterName != null) {
            this.chapterName = other.chapterName
        }

        if (other.chapterPath != null) {
            this.chapterPath = other.chapterPath
        }

        if (other.comic != null) {
            this.comic = other.comic
        }

        if (other.pages != null) {
            this.pages = other.pages
        }
    }

    fun copyFrom(other: IChapter) {
        this.lastPageRead = other.lastPageRead

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.chapterPath != null) {
            this.chapterPath = other.chapterPath
        }

        if (other.comic != null) {
            this.comic = other.comic
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(chapterName)
        parcel.writeString(chapterPath)
        parcel.writeInt(lastPageRead)
        parcel.writeTypedList(pages)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChapterViewModel> {
        override fun createFromParcel(parcel: Parcel): ChapterViewModel {
            return ChapterViewModel(parcel)
        }

        override fun newArray(size: Int): Array<ChapterViewModel?> {
            return arrayOfNulls(size)
        }
    }
}