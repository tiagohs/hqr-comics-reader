package com.tiagohs.hqr.models.database.comics

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Chapter: RealmObject() {

    @PrimaryKey
    var id: Long = 0L
    var name: String? = ""
    var chapterPath: String? = ""
    var pages: RealmList<Page>? = null
    var sourceOrder: Int? = 0

    var comic: Comic? = null
}