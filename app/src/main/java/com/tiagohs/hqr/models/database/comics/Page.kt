package com.tiagohs.hqr.models.database.comics

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Page: RealmObject() {

    @PrimaryKey
    var id: Long = 0L
    var index: Int? = 0
    var url: String? = ""
    var imageUrl: String? = null
}