package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic

interface IComicHistory{

    var id: Long
    var lastTimeRead: String?

    var comic: Comic?
    var chapter: Chapter?

    fun copyFrom(other: IComicHistory) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (!other.lastTimeRead.isNullOrEmpty()) {
            this.lastTimeRead = other.lastTimeRead
        }

        if (other.comic != null) {
            this.comic = other.comic
        }

        if (other.chapter != null) {
            this.chapter = other.chapter
        }

    }
}