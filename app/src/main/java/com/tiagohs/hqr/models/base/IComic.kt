package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import io.realm.RealmList

interface IComic: IDefaultModel {

    var pathLink: String
    var posterPath: String?
    var summary: String?
    var publicationDate: String?

    var publisher: RealmList<DefaultModel>?
    var genres: RealmList<DefaultModel>?
    var authors: RealmList<DefaultModel>?
    var scanlators: RealmList<DefaultModel>?
    var chapters: RealmList<Chapter>?

    var inicialized: Boolean
    var favorite: Boolean
    var lastUpdate: String?

    var tags: RealmList<String>?

    var source: SourceDB?

    fun copyFrom(other: IComic) {
        super.copyFrom(other)

        this.source = other.source
        this.pathLink = other.pathLink
        this.posterPath = other.posterPath
        this.summary = other.posterPath
        this.publicationDate = other.posterPath

        this.publisher = other.publisher
        this.genres = other.genres
        this.authors = other.authors
        this.scanlators = other.scanlators
        this.chapters = other.chapters

        this.inicialized = other.inicialized
        this.favorite = other.favorite
        this.lastUpdate = other.lastUpdate

        this.tags = other.tags
    }

    fun containsTag(tag: String): Boolean {
        return tags?.contains(tag) ?: false
    }

    companion object {
        const val POPULARS = 0
        const val RECENTS = 1
    }
}