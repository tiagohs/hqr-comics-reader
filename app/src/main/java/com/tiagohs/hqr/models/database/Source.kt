package com.tiagohs.hqr.models.database

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Source: RealmObject() {

    @PrimaryKey
    var id: Long = 0L

    var name: String? = ""
    var baseUrl: String = ""
    var language: String = ""

    var hasPageSupport: Boolean = false
    var hasThumbnailSupport: Boolean = false

}