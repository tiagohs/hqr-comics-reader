package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.LocaleDTO

interface ISource {
    val id: Long
    val name: String
    val baseUrl: String
    val language: LocaleDTO

    val hasPageSupport: Boolean
    val hasThumbnailSupport: Boolean
}