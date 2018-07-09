package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.DefaultModelView

interface IDefaultModel{
    var id: Long
    var name: String?
    var pathLink: String?
    var type: String?

    var source: SourceDB?

    fun copyFrom(other: IDefaultModel, sourceDB: SourceDB?) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

        if (other.type != null) {
            this.type = other.type
        }

        if (sourceDB != null) {
            this.source = sourceDB
        }
    }

    fun copyFrom(other: DefaultModelView, sourceDB: SourceDB?) {

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

        if (other.type != null) {
            this.type = other.type
        }

        if (sourceDB != null) {
            this.source = sourceDB
        }
    }

}