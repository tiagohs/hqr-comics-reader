package com.tiagohs.hqr.models.base

import com.tiagohs.hqr.models.view_models.DefaultModelView

interface IDefaultModel{
    var id: Long
    var name: String?
    var pathLink: String?

    fun copyFrom(other: IDefaultModel) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }
    }

    fun copyFrom(other: DefaultModelView) {

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }
    }


}