package com.tiagohs.hqr.models.base

interface ISource{

    var id: Long

    var name: String?
    var baseUrl: String
    var language: String

    var hasPageSupport: Boolean
    var hasThumbnailSupport: Boolean

    fun isCurrentSelect(currrentSourceId: Long): Boolean {
        return id == currrentSourceId
    }

    fun copyFrom(other: ISource) {
        this.id = other.id
        this.name = other.name
        this.baseUrl = other.baseUrl
        this.language = other.language
        this.hasPageSupport = other.hasPageSupport
        this.hasThumbnailSupport = other.hasThumbnailSupport
    }
}