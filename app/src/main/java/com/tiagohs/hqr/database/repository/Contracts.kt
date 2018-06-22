package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.Source
import io.realm.RealmResults

interface ISourceRepository {

    fun insert(source: Source)
    fun getSources(): RealmResults<Source>
    fun getSourcesAndLanguages(): RealmResults<CatalogueSource>
}