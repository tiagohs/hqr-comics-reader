package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

private const val SITE = "https://mangadex.org"
private const val COVERS = "https://uploads.mangadex.org/covers"

/** API answers to HQR models; [preferred] languages pick titles, descriptions and tag names (RF-29). */
internal fun MangaDto.toManga(preferred: List<String>): SourceManga {
    val titles = listOf(attributes.title) + attributes.altTitles
    val title =
        preferred.firstNotNullOfOrNull { lang -> titles.firstNotNullOfOrNull { it[lang] } } ?: attributes.title.values.firstOrNull()
            ?: id
    val cover = relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
    val descriptions = (attributes.description as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
    return SourceManga(
        sourceId = SourceId.MANGADEX,
        key = id,
        title = title,
        coverUrl = cover?.let { "$COVERS/$id/$it.512.jpg" },
        url = "$SITE/title/$id",
        alternativeTitles = titles.flatMap { it.values }.distinct().filter { it != title },
        authors = relationships.filter { it.type == "author" }.mapNotNull { it.attributes?.name }.distinct(),
        artists = relationships.filter { it.type == "artist" }.mapNotNull { it.attributes?.name }.distinct(),
        status = mangaDexStatus(attributes.status),
        genres = attributes.tags.mapNotNull { tag -> preferred.firstNotNullOfOrNull { tag.attributes.name[it] } },
        description = preferred.firstNotNullOfOrNull { descriptions[it] }?.trim(),
        language = attributes.originalLanguage,
        kind = mangaDexKind(attributes.originalLanguage),
    )
}

internal fun ChapterDto.toChapter(): SourceChapter =
    SourceChapter(
        key = id,
        number = attributes.chapter?.toDoubleOrNull() ?: 0.0,
        title = attributes.title?.takeIf { it.isNotBlank() },
        volume = attributes.volume,
        language = attributes.translatedLanguage,
        group = relationships.firstOrNull { it.type == "scanlation_group" }?.attributes?.name,
        groupUrl = relationships.firstOrNull { it.type == "scanlation_group" }?.let { "$SITE/group/${it.id}" },
        uploadedAtEpochMs =
            (attributes.publishAt ?: attributes.readableAt)
                ?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }?.toEpochMilliseconds(),
        externalUrl = attributes.externalUrl,
        pageCount = attributes.pages.takeIf { it > 0 },
    )

internal fun mangaDexStatus(value: String?): PublicationStatus =
    when (value) {
        "ongoing" -> PublicationStatus.ONGOING
        "completed" -> PublicationStatus.COMPLETED
        "hiatus" -> PublicationStatus.HIATUS
        "cancelled" -> PublicationStatus.CANCELLED
        else -> PublicationStatus.UNKNOWN
    }

internal fun mangaDexKind(originalLanguage: String?): ReadingKind =
    when (originalLanguage) {
        "ja" -> ReadingKind.MANGA
        "ko", "zh", "zh-hk" -> ReadingKind.MANHWA
        null -> ReadingKind.UNKNOWN
        else -> ReadingKind.COMIC
    }
