package com.tiagohs.hqr.models.database

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class CatalogueSource: RealmObject() {

    @PrimaryKey
    var id: Long = 0L
    var language: String? = null

    var sources: RealmList<Source>? = null
}