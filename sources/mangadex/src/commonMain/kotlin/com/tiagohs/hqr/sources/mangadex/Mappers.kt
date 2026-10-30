package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.isoEpochMs
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
        kind = mangaDexKind(attributes.originalLanguage, attributes.tags.any { it.attributes.name["en"] == LONG_STRIP }),
        updatedAtEpochMs = isoEpochMs(attributes.updatedAt),
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

private const val LONG_STRIP = "Long Strip"

/**
 * RF-37: the first-read suggestion. Japanese manga reads right to left; the "Long Strip" tag (most manhwa and manhua)
 * is a webtoon; a Korean or Chinese work without it pages left to right, like a western comic.
 */
internal fun mangaDexKind(
    originalLanguage: String?,
    longStrip: Boolean = false,
): ReadingKind =
    when {
        longStrip -> ReadingKind.MANHWA
        originalLanguage == "ja" -> ReadingKind.MANGA
        originalLanguage == null -> ReadingKind.UNKNOWN
        else -> ReadingKind.COMIC
    }

/** RF-08: whatever the caller asks, erotica and pornographic never go out. */
internal fun allowedRatings(requested: Set<String>): Set<String> =
    requested.intersect(setOf("safe", "suggestive")).ifEmpty { setOf("safe") }
