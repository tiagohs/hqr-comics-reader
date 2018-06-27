package com.tiagohs.hqr.models.database

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IDefaultModel
import com.tiagohs.hqr.models.database.comics.Comic
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class DefaultModel: RealmObject(), IDefaultModel {

    @PrimaryKey
    override var id: Long = -1L
    override var name: String? = ""
    override var pathLink: String? = ""

    fun create(): DefaultModel {
        return DefaultModel().apply {
            this.id = RealmUtils.getDataId<Comic>()
        }
    }
}