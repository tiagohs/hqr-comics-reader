package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage

/**
 * The single source contract (RF-01, ADR-002): Explore, search, details and the reader only know this.
 * Optional capabilities are extra interfaces ([CatalogSource], [SearchSource]).
 */
interface Source {
    val id: SourceId
    val name: String

    /** Content languages the source offers ("pt-br", "en"); empty when it has no language (local files). */
    val languages: List<String>

    /** Version of the key format; when it grows, saved keys are migrated (RF-91). */
    val schemaVersion: Int get() = 1

    suspend fun details(manga: SourceManga): SourceManga

    suspend fun chapters(manga: SourceManga): List<SourceChapter>

    suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage>
}

/** A source with popular and recent listings (RF-21). */
interface CatalogSource : Source {
    suspend fun popular(page: Int): MangaPage

    suspend fun latest(page: Int): MangaPage
}

/** A source that searches by text and filters (RF-22). */
interface SearchSource : Source {
    /** The filters this source offers, at their defaults; may ask the network once (MangaDex tags). */
    suspend fun filters(): List<SourceFilter> = emptyList()

    suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage
}

/** Thrown by a source for a failure that belongs to that source only (RF-05). */
open class SourceException(
    message: String,
    val kind: Kind,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Kind { NETWORK, SERVER, PARSE, NOT_FOUND, AUTH, CHALLENGE, UNAVAILABLE }
}

/**
 * A source that fetches its own images (limits, reports, signed addresses): MangaDex at-home (RF-09) or the local
 * files. Without it, images are a plain GET with the page headers.
 */
interface ImageSource {
    suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray
}
