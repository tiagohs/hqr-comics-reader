package com.tiagohs.hqr.models.database

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.ISource
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class SourceDB: ISource,  RealmObject() {

    @PrimaryKey
    override var id: Long = -1L

    override var name: String? = ""
    override var baseUrl: String = ""
    override var language: String = ""

    override var hasThumbnailSupport: Boolean = false

    override var lastPopularUpdate: String? = null
    override var lastLastestUpdate: String? = null

    override var hasInAllPageSupport: Boolean = false
    override var hasInPublisherPageSupport: Boolean = false
    override var hasInScanlatorPageSupport: Boolean = false
    override var hasInGenresPageSupport: Boolean = false


    fun create(): SourceDB {
        return SourceDB().apply {
            this.id = RealmUtils.getDataId<SourceDB>()
        }
    }

    fun create(other: SourceDB): SourceDB {
        return SourceDB().apply {
            copyFrom(other)
        }
    }

    fun createList(others: List<SourceDB>): List<SourceDB> {
        val finalResults = ArrayList<SourceDB>();

        others.forEach {
            finalResults.add(SourceDB().create(it))
        }

        return finalResults.toList()
    }

}