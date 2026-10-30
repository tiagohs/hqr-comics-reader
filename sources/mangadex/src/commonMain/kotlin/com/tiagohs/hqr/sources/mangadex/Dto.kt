package com.tiagohs.hqr.sources.mangadex

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Only the fields HQR reads from the MangaDex API v5 (https://api.mangadex.org/docs/).

@Serializable
internal data class ListResponse<T>(
    val data: List<T> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
internal data class EntityResponse<T>(
    val data: T,
)

@Serializable
internal data class MangaDto(
    val id: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
internal data class MangaAttributes(
    val title: Map<String, String> = emptyMap(),
    val altTitles: List<Map<String, String>> = emptyList(),
    val description: JsonElement? = null,
    val originalLanguage: String? = null,
    val status: String? = null,
    val contentRating: String? = null,
    val tags: List<TagDto> = emptyList(),
    val updatedAt: String? = null,
)

@Serializable
internal data class TagDto(
    val id: String,
    val attributes: TagAttributes,
)

@Serializable
internal data class TagAttributes(
    val name: Map<String, String> = emptyMap(),
    val group: String? = null,
)

@Serializable
internal data class Relationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null,
)

@Serializable
internal data class RelationshipAttributes(
    val name: String? = null,
    val fileName: String? = null,
    val website: String? = null,
)

@Serializable
internal data class ChapterDto(
    val id: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
internal data class ChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String? = null,
    val externalUrl: String? = null,
    val publishAt: String? = null,
    val readableAt: String? = null,
    val pages: Int = 0,
)

@Serializable
internal data class AtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter,
)

@Serializable
internal data class AtHomeChapter(
    val hash: String,
    val data: List<String> = emptyList(),
    val dataSaver: List<String> = emptyList(),
)

@Serializable
internal data class ReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Int,
    val duration: Long,
    val cached: Boolean,
)
