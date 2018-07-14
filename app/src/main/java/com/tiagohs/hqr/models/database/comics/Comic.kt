package com.tiagohs.hqr.models.database.comics

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.base.IComic
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Comic: RealmObject(), IComic {

    @PrimaryKey
    override var id: Long = -1L

    override var name: String? = ""
    override var pathLink: String? = ""
    override var posterPath: String? = ""
    override var summary: String? = ""
    override var publicationDate: String? = ""

    override var publisher: RealmList<DefaultModel>? = null
    override var genres: RealmList<DefaultModel>? = null
    override var authors: RealmList<DefaultModel>? = null
    override var scanlators: RealmList<DefaultModel>? = null
    override var chapters: RealmList<Chapter>? = null

    override var downloaded: Boolean = false
    override var inicialized: Boolean = false
    override var favorite: Boolean = false
    override var lastUpdate: String? = ""

    override var status: String? = ""
        set(value) {
            field = ScreenUtils.getStatusConstant(value)
        }

    override var tags: RealmList<String>? = null

    override var source: SourceDB? = null

    fun create(): Comic {
        return Comic().apply {
            this.id = RealmUtils.getDataId<Comic>()
        }
    }

    fun create(other: Comic): Comic {
        return Comic().apply {
            copyFrom(other)
        }
    }

    fun create(other: ComicViewModel): Comic {
        return Comic().apply {
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

    fun createList(others: List<Comic>): List<Comic> {
        val finalList = ArrayList<Comic>()

        others.forEach {
            finalList.add(Comic().create(it))
        }

        return finalList.toList()
    }
}