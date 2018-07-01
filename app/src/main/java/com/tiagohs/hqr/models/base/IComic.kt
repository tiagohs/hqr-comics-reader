package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.realm.RealmList

interface IComic: IDefaultModel {

    var posterPath: String?
    var summary: String?
    var publicationDate: String?

    var publisher: RealmList<DefaultModel>?
    var genres: RealmList<DefaultModel>?
    var authors: RealmList<DefaultModel>?
    var scanlators: RealmList<DefaultModel>?
    var chapters: RealmList<Chapter>?
    var status: String?

    var inicialized: Boolean
    var favorite: Boolean
    var lastUpdate: String?

    var tags: RealmList<String>?

    var source: SourceDB?

    fun copyFrom(other: IComic) {
        super.copyFrom(other)

        if (other.source != null) {
            this.source = other.source
        }

        if (other.posterPath != null) {
            this.posterPath = other.posterPath
        }

        if (other.summary != null) {
            this.summary = other.summary
        }

        if (other.publicationDate != null) {
            this.publicationDate = other.publicationDate
        }

        if (other.publisher != null) {
            this.publisher = other.publisher
        }

        if (other.genres != null) {
            this.genres = other.genres
        }

        if (other.authors != null) {
            this.authors = other.authors
        }

        if (other.scanlators != null) {
            this.scanlators = other.scanlators
        }

        if (other.chapters != null) {
            this.chapters = other.chapters
        }

        if (other.lastUpdate != null) {
            this.lastUpdate = other.lastUpdate
        }

        if (other.status != null) {
            this.status = other.status
        }

        if (other.tags != null) {
            this.tags = other.tags
        }

        this.pathLink = other.pathLink
        this.inicialized = other.inicialized
        this.favorite = other.favorite

    }

    fun copyFrom(other: ComicViewModel) {

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

        if (other.posterPath != null) {
            this.posterPath = other.posterPath
        }

        if (other.summary != null) {
            this.summary = other.summary
        }

        if (other.publicationDate != null) {
            this.publicationDate = other.publicationDate
        }

        if (other.status != null) {
            this.status = other.status
        }

        this.pathLink = other.pathLink
    }

    fun containsTag(tag: String): Boolean {
        return tags?.contains(tag) ?: false
    }

    companion object {
        const val POPULARS = "POPULARS"
        const val RECENTS = "RECENTS"
    }
}