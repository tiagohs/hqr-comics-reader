package com.tiagohs.hqr.models.base

import io.realm.RealmList

interface ICatalogueSource<T: ISource>{

    var id: Long
    var language: String?
    var sourceDBS: RealmList<T>?

    fun copyFrom(other: ICatalogueSource<T>) {
        id = other.id

        if (other.language != null) {
            language = other.language
        }

        if (other.sourceDBS != null) {
            sourceDBS = other.sourceDBS
        }
    }
}