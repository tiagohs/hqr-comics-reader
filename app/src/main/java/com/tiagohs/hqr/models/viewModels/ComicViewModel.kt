package com.tiagohs.hqr.models.viewModels

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic

class ComicViewModel {
    var id: Long = -1L
    var name: String? = ""
    var pathLink: String? = ""
    var posterPath: String? = ""
    var summary: String? = ""
    var publicationDate: String? = ""

    var publisher: List<DefaultModel>? = null
    var genres: List<DefaultModel>? = null
    var authors: List<DefaultModel>? = null
    var scanlators: List<DefaultModel>? = null
    var chapters: List<Chapter>? = null

    var inicialized: Boolean = false
    var favorite: Boolean = false
    var lastUpdate: String? = ""

    var status: String? = ""
        set(value) {
            field = ScreenUtils.getStatusConstant(value)
        }

    var tags: List<String>? = null
    var source: SourceDB? = null

    fun create(other: Comic): ComicViewModel {
        return ComicViewModel().apply {
            copyFrom(other)
        }
    }

    fun create(name: String?, posterPath: String?): Comic {
        return Comic().apply {
            this.id = RealmUtils.getDataId<Comic>()
            this.name = name
            this.posterPath = posterPath
        }
    }

    fun createList(others: List<Comic>): List<ComicViewModel> {
        val finalList = ArrayList<ComicViewModel>()

        others.forEach {
            finalList.add(ComicViewModel().create(it))
        }

        return finalList.toList()
    }

    fun copyFrom(other: IComic) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

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
            this.publisher = other.publisher!!.toList()
        }

        if (other.genres != null) {
            this.genres = other.genres!!.toList()
        }

        if (other.authors != null) {
            this.authors = other.authors!!.toList()
        }

        if (other.scanlators != null) {
            this.scanlators = other.scanlators!!.toList()
        }

        if (other.chapters != null) {
            this.chapters = other.chapters!!.toList()
        }

        if (other.lastUpdate != null) {
            this.lastUpdate = other.lastUpdate
        }

        if (other.tags != null) {
            this.tags = other.tags!!.toList()
        }

        this.pathLink = other.pathLink
        this.inicialized = other.inicialized
        this.favorite = other.favorite

    }

    fun copyFrom(other: ComicViewModel) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

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
            this.publisher = other.publisher!!.toList()
        }

        if (other.genres != null) {
            this.genres = other.genres!!.toList()
        }

        if (other.authors != null) {
            this.authors = other.authors!!.toList()
        }

        if (other.scanlators != null) {
            this.scanlators = other.scanlators!!.toList()
        }

        if (other.chapters != null) {
            this.chapters = other.chapters!!.toList()
        }

        if (other.lastUpdate != null) {
            this.lastUpdate = other.lastUpdate
        }

        if (other.tags != null) {
            this.tags = other.tags!!.toList()
        }

        this.pathLink = other.pathLink
        this.inicialized = other.inicialized
        this.favorite = other.favorite

    }

}