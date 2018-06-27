package com.tiagohs.hqr.models.base

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


}