package com.tiagohs.hqr.models.database

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IDefaultModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class DefaultModel: RealmObject(), IDefaultModel {

    companion object {
        val PUBLISHER: String = "PUBLISHER"
        val GENRE: String = "GENRE"
        val SCANLATOR: String = "SCANLATOR"
        val AUTHOR: String = "AUTHOR"
    }

    @PrimaryKey
    override var id: Long = -1L

    override var name: String? = ""
    override var pathLink: String? = ""

    override var source: SourceDB? = null

    override var type: String? = ""

    fun create(): DefaultModel {
        return DefaultModel().apply {
            this.id = RealmUtils.getDataId<DefaultModel>()
        }
    }
}