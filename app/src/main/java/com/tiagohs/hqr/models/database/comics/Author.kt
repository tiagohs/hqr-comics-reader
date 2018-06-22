package com.tiagohs.hqr.models.database.comics

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Author: RealmObject() {

    @PrimaryKey
    var id: Long = 0L
    var name: String? = ""
}