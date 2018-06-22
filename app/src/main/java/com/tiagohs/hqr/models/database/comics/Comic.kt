package com.tiagohs.hqr.models.database.comics

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Comic: RealmObject() {

    @PrimaryKey
    var id: Long = 0L
    var name: String? = ""
    var pathLink: String = ""
    var posterPath: String? = ""
    var summary: String?= ""
    var publicationDate: Date?= null

    var publisher: RealmList<Publisher>? = null
    var genres: RealmList<Genrer>? = null
    var authors: RealmList<Author>? = null
    var chapters: RealmList<Chapter>? = null
    var scanlators: RealmList<Scanlator>? = null
}