package com.tiagohs.hqr.models.base

interface IDefaultModel{
    var id: Long
    var name: String?

    fun copyFrom(other: IDefaultModel) {
        this.id = other.id
        this.name = other.name
    }
}