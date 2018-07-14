package com.tiagohs.hqr.models.view_models

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.models.base.IChapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.ui.adapters.chapters.ChapterItem

class ChapterViewModel() : Parcelable {

    var id: Long = -1L
    var chapterName: String? = ""
    var chapterPath: String? = ""
    var lastPageRead: Int = 0

    var pages: List<Page>? = null
    var comic: Comic? = null

    var downloaded: Boolean = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        chapterName = parcel.readString()
        chapterPath = parcel.readString()
        lastPageRead = parcel.readInt()
        pages = parcel.createTypedArrayList(Page)
        downloaded = parcel.readByte() != 0.toByte()
    }

    fun create(other: ChapterItem): ChapterViewModel {
        return ChapterViewModel().apply {
            copyFrom(other)
        }
    }

    fun create(other: IChapter): ChapterViewModel {
        return ChapterViewModel().apply {
            copyFrom(other)
        }
    }

    fun copyFrom(other: ChapterItem) {
        this.lastPageRead = other.chapter.lastPageRead

        if (other.chapter.id != -1L) {
            this.id = other.chapter.id
        }

        if (other.chapter.chapterName != null) {
            this.chapterName = other.chapter.chapterName
        }

        if (other.chapter.chapterPath != null) {
            this.chapterPath = other.chapter.chapterPath
        }

        if (other.comic != null) {
            this.comic = Comic().create(other.comic)
        }

        if (other.chapter.pages != null) {
            this.pages = other.chapter.pages
        }

        this.downloaded = other.chapter.downloaded
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
            this.comic = Comic().create(other.comic!!)
        }

        if (other.pages != null) {
            this.pages = other.pages
        }

        this.downloaded = other.downloaded
    }

    fun copyFrom(other: IChapter) {
        this.lastPageRead = other.lastPageRead
        this.downloaded = other.downloaded

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
            this.comic = Comic().create(other.comic!!)
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(chapterName)
        parcel.writeString(chapterPath)
        parcel.writeInt(lastPageRead)
        parcel.writeTypedList(pages)
        parcel.writeByte(if (downloaded) 1 else 0)
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