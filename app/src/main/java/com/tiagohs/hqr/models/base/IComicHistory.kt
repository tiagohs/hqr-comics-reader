package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic

interface IComicHistory{

    var id: Long
    var lastTimeRead: String?

    var comic: Comic?
    var chapter: Chapter?

    fun copyFrom(other: IComicHistory) {
        this.id = other.id
        this.lastTimeRead = other.lastTimeRead
        this.comic = other.comic
        this.chapter = other.chapter
    }
}