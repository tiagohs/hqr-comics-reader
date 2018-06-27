package com.tiagohs.hqr.models.base

import io.realm.RealmList

interface ICatalogueSource<T: ISource>{

    var id: Long
    var language: String?
    var sourceDBS: RealmList<T>?

    fun copyFrom(other: ICatalogueSource<T>) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.language != null) {
            this.language = other.language
        }

        if (other.sourceDBS != null) {
            this.sourceDBS = other.sourceDBS
        }

    }

}