package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.comics.Comic

interface IChapter{

    var id: Long
    var chapterPath: String?
    var lastPageRead: Int

    var comic: Comic?

    fun copyFrom(other: IChapter) {
        this.id = other.id
        this.chapterPath = other.chapterPath
        this.comic = other.comic
        this.lastPageRead = other.lastPageRead
    }

}