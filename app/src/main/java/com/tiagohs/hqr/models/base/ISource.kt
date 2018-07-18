package com.tiagohs.hqr.models.base

interface ISource{

    var id: Long

    var name: String?
    var baseUrl: String
    var language: String

    var hasThumbnailSupport: Boolean

    var lastPopularUpdate: String?
    var lastLastestUpdate: String?

    var hasInAllPageSupport: Boolean
    var hasInPublisherPageSupport: Boolean
    var hasInScanlatorPageSupport: Boolean
    var hasInGenresPageSupport: Boolean

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
        this.hasThumbnailSupport = other.hasThumbnailSupport

        this.hasInAllPageSupport = other.hasInAllPageSupport
        this.hasInGenresPageSupport = other.hasInGenresPageSupport
        this.hasInPublisherPageSupport = other.hasInPublisherPageSupport
        this.hasInScanlatorPageSupport = other.hasInScanlatorPageSupport
    }
}