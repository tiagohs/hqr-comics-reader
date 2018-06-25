package com.tiagohs.hqr.models.database

import com.tiagohs.hqr.models.base.IDefaultModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class DefaultModel: RealmObject(), IDefaultModel {

    @PrimaryKey
    override var id: Long = 0L
    override var name: String? = ""
}