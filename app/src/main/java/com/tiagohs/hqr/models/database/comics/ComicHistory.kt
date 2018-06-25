package com.tiagohs.hqr.models.database.comics

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IComicHistory
import io.realm.RealmObject

open class ComicHistory: RealmObject(), IComicHistory {

    override var id: Long = -1L
    override var lastTimeRead: String? = ""
    override var comic: Comic? = null
    override var chapter: Chapter? = null

    fun create(): ComicHistory {
        return ComicHistory().apply {
            this.id = RealmUtils.getDataId<ComicHistory>()
        }
    }

    fun create(other: ComicHistory): ComicHistory {
        return ComicHistory().apply {
            copyFrom(other)
        }
    }

    fun createList(others: List<ComicHistory>): List<ComicHistory> {
        val finalList = ArrayList<ComicHistory>()

        others.forEach {
            finalList.add(ComicHistory().create(it))
        }

        return finalList.toList()
    }
}