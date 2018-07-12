package com.tiagohs.hqr.models.view_models

import com.tiagohs.hqr.factory.ComicsFactory
import com.tiagohs.hqr.models.database.comics.ComicHistory

class ComicHistoryViewModel {

    var id: Long = -1L
    var lastTimeRead: String? = ""
    var comic: ComicViewModel? = null
    var chapter: ChapterViewModel? = null

    fun create(other: ComicHistoryViewModel): ComicHistoryViewModel {
        return ComicHistoryViewModel().apply {
            copyFrom(other)
        }
    }

    fun create(other: ComicHistory): ComicHistoryViewModel {
        return ComicHistoryViewModel().apply {
            copyFrom(other)
        }
    }

    fun copyFrom(other: ComicHistoryViewModel) {

        if (!other.lastTimeRead.isNullOrEmpty() ) {
            this.lastTimeRead = other.lastTimeRead
        }

        if (other.comic != null) {
            this.comic = other.comic
        }

        if (other.chapter != null) {
            this.chapter = other.chapter
        }
    }

    fun copyFrom(other: ComicHistory) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (!other.lastTimeRead.isNullOrEmpty() ) {
            this.lastTimeRead = other.lastTimeRead
        }

        if (other.comic != null) {
            this.comic = ComicsFactory.createComicViewModel(other.comic!!)
        }

        if (other.chapter != null) {
            this.chapter = ChapterViewModel().create(other.chapter!!)
        }
    }
}