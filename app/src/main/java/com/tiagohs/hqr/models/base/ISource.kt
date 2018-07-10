package com.tiagohs.hqr.models.base

interface ISource{

    var id: Long

    var name: String?
    var baseUrl: String
    var language: String

    var hasPageSupport: Boolean
    var hasThumbnailSupport: Boolean

    var lastPopularUpdate: String?
    var lastLastestUpdate: String?

    fun isCurrentSelect(currrentSourceId: Long): Boolean {
        return id == currrentSourceId
    }

    fun copyFrom(other: ISource) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.lastLastestUpdate != null) {
            this.lastLastestUpdate = other.lastLastestUpdate
        }

        if (other.lastPopularUpdate != null) {
            this.lastPopularUpdate = other.lastPopularUpdate
        }

        this.baseUrl = other.baseUrl
        this.language = other.language
        this.hasPageSupport = other.hasPageSupport
        this.hasThumbnailSupport = other.hasThumbnailSupport
    }
}