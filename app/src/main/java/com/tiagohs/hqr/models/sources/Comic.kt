package com.tiagohs.hqr.models.sources

class Comic(
        val title: String?,
        val posterPath: String?,
        val status: String?,
        val publisher: List<SimpleItem>?,
        val genres: List<SimpleItem>?,
        val writers: List<SimpleItem>?,
        val artists: List<SimpleItem>?,
        val chapters: List<SimpleItem>?,
        val summary: String?,
        val publicationDate: String?,
        val scanlators: List<SimpleItem>?) {
}