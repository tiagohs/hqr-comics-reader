package com.tiagohs.hqr.models.database

import com.tiagohs.hqr.models.base.ICatalogueSource
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class CatalogueSource() : ICatalogueSource<SourceDB>, RealmObject() {

    @PrimaryKey
    override var id: Long = 0L
    override var language: String? = null
    override var sourceDBS: RealmList<SourceDB>? = null

    fun create(other: CatalogueSource): CatalogueSource {
        return CatalogueSource().apply {
            this.id = other.id
            this.language = other.language
            this.sourceDBS = createSources(other.sourceDBS)
        }
    }

    fun createList(others: List<CatalogueSource>): List<CatalogueSource> {
        val finalList = ArrayList<CatalogueSource>()

        others.forEach {
            finalList.add(CatalogueSource().create(it))
        }

        return finalList.toList()
    }

    private fun createSources(other: RealmList<SourceDB>?): RealmList<SourceDB> {
        val sources = RealmList<SourceDB>()

        other?.forEach {
            sources.add(SourceDB().create(it))
        }

        return sources
    }

}