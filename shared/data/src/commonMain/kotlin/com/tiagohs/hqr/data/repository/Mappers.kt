package com.tiagohs.hqr.data.repository

import com.tiagohs.hqr.data.db.ChapterEntity
import com.tiagohs.hqr.data.db.MangaEntity
import com.tiagohs.hqr.data.db.ProgressEntity
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal val json = Json { ignoreUnknownKeys = true }
private val stringList = ListSerializer(String.serializer())

internal fun List<String>.toJson(): String = json.encodeToString(stringList, this)

internal fun String.toStringList(): List<String> = if (isBlank()) emptyList() else json.decodeFromString(stringList, this)

internal fun MangaEntity.toSource(): SourceManga =
    SourceManga(
        sourceId = SourceId(sourceId),
        key = key,
        title = title,
        coverUrl = coverUrl,
        url = url,
        alternativeTitles = alternativeTitles.toStringList(),
        authors = authors.toStringList(),
        artists = artists.toStringList(),
        status = runCatching { PublicationStatus.valueOf(status) }.getOrDefault(PublicationStatus.UNKNOWN),
        genres = genres.toStringList(),
        description = description,
        language = language,
        kind = runCatching { ReadingKind.valueOf(kind) }.getOrDefault(ReadingKind.UNKNOWN),
    )

internal fun MangaEntity.toDomain(): Manga =
    Manga(id = id, source = toSource(), inLibrary = inLibrary, addedAtEpochMs = addedAt, coverColor = coverColor)

internal fun SourceManga.toEntity(now: Long): MangaEntity =
    MangaEntity(
        sourceId = sourceId.value,
        key = key,
        url = url,
        title = title,
        alternativeTitles = alternativeTitles.toJson(),
        coverUrl = coverUrl,
        authors = authors.toJson(),
        artists = artists.toJson(),
        status = status.name,
        genres = genres.toJson(),
        language = language,
        description = description,
        kind = kind.name,
        inLibrary = false,
        addedAt = null,
        coverColor = null,
        updatedAt = now,
    )

internal fun ChapterEntity.toDomain(
    progress: ProgressEntity?,
    downloaded: Boolean,
): Chapter =
    Chapter(
        id = id,
        mangaId = mangaId,
        source =
            SourceChapter(
                key = key,
                number = number,
                title = title,
                volume = volume,
                language = language,
                group = group,
                groupUrl = groupUrl,
                uploadedAtEpochMs = uploadedAt,
                externalUrl = externalUrl,
                pageCount = progress?.pageCount,
            ),
        fetchedAtEpochMs = fetchedAt,
        read = progress?.read ?: false,
        lastPage = progress?.page ?: 0,
        pageCount = progress?.pageCount,
        removedAtSource = removedAtSource,
        downloaded = downloaded,
    )
