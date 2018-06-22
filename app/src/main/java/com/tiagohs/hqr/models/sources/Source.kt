package com.tiagohs.hqr.models.sources

class Source {
    var id: String? = ""
    var name: String? = ""
    var baseUrl: String? = ""
    var language: LocaleDTO? = null
    var hasPageSupport: Boolean = false
    var hasThumbnailSupport: Boolean = false

}