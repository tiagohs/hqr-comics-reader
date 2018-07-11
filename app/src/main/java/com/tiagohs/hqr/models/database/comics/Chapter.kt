package com.tiagohs.hqr.models.database.comics

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IChapter
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Chapter: RealmObject(), IChapter {

    @PrimaryKey
    override var id: Long = -1L

    override var chapterName: String? = ""
    override var chapterPath: String? = ""
    override var lastPageRead: Int = -1

    override var comic: Comic? = null

    fun create(): Chapter {
        return Chapter().apply {
            this.id = RealmUtils.getDataId<Chapter>()
        }
    }

    fun create(other: Chapter): Chapter {
        return Chapter().apply {
            copyFrom(other)
        }
    }

    fun createList(others: List<Chapter>): List<Chapter> {
        val finalList = ArrayList<Chapter>()

        others.forEach {
            finalList.add(Chapter().create(it))
        }

        return finalList.toList()
    }

}