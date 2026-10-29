package com.tiagohs.hqr.domain.model

import kotlinx.serialization.Serializable

/** Publication state of a work as the source reports it (RF-29). Never used as a library category. */
@Serializable
enum class PublicationStatus { ONGOING, COMPLETED, CANCELLED, HIATUS, UNKNOWN }

/** Reading direction suggested by the source for the first read (RF-37). */
@Serializable
enum class ReadingKind { MANGA, MANHWA, COMIC, UNKNOWN }

/**
 * A work as a source describes it. [key] is the source's own identifier (a MangaDex UUID, an HQ Now id, a path
 * relative to the chosen folder for local files) and, with [sourceId], identifies the work (RF-90).
 */
@Serializable
data class SourceManga(
    val sourceId: SourceId,
    val key: String,
    val title: String,
    val coverUrl: String? = null,
    val url: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val status: PublicationStatus = PublicationStatus.UNKNOWN,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val language: String? = null,
    val kind: ReadingKind = ReadingKind.UNKNOWN,
)

/** A chapter (or issue, or volume) as a source lists it. [number] is decimal (10.5 exists). */
@Serializable
data class SourceChapter(
    val key: String,
    val number: Double,
    val title: String? = null,
    val volume: String? = null,
    val language: String? = null,
    val group: String? = null,
    val groupUrl: String? = null,
    val uploadedAtEpochMs: Long? = null,
    val externalUrl: String? = null,
    val pageCount: Int? = null,
)

/** One page of a chapter: where to fetch the image, plus headers the source requires. */
@Serializable
data class SourcePage(
    val index: Int,
    val imageUrl: String,
    val headers: Map<String, String> = emptyMap(),
)

/** One page of a catalog listing with continuous loading (RF-21). */
data class MangaPage(
    val items: List<SourceManga>,
    val hasNextPage: Boolean,
)
