package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.Locale

interface ISource {
    val id: Long
    val name: String
    val baseUrl: String
    val language: Locale

    val hasPageSupport: Boolean
    val hasThumbnailSupport: Boolean
}