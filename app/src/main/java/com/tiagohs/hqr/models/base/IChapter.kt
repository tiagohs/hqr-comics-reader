package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.comics.Comic

interface IChapter{

    var id: Long
    var chapterPath: String?
    var lastPageRead: Int

    var comic: Comic?

    fun copyFrom(other: IChapter) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.chapterPath != null) {
            this.chapterPath = other.chapterPath
        }

        if (other.comic != null) {
            this.comic = other.comic
        }

        this.lastPageRead = other.lastPageRead
    }

}