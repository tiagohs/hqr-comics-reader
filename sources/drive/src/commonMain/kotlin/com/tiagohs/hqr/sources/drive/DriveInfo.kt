package com.tiagohs.hqr.sources.drive

import com.tiagohs.hqr.domain.model.PublicationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The `info.json` in a work's folder (RF-130). Every field is optional: a work with no file, or a broken one, still
 * opens with the folder's name.
 */
@Serializable
data class DriveInfo(
    val title: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    /** "ongoing", "completed", "hiatus" or "cancelled" (also "em andamento", "completo", "pausado", "cancelado"). */
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    /** "pt-br", "en"… */
    val language: String? = null,
    /** Chapter folder name → chapter title ("01": "O começo"); the chapters not listed are just their number. */
    val chapters: Map<String, String> = emptyMap(),
) {
    fun publicationStatus(): PublicationStatus =
        when (status?.trim()?.lowercase()) {
            "ongoing", "em andamento", "andamento" -> PublicationStatus.ONGOING
            "completed", "complete", "completo", "completa", "finalizado", "finalizada" -> PublicationStatus.COMPLETED
            "hiatus", "pausado", "pausada" -> PublicationStatus.HIATUS
            "cancelled", "canceled", "cancelado", "cancelada" -> PublicationStatus.CANCELLED
            else -> PublicationStatus.UNKNOWN
        }

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        /** The file's JSON, or an empty [DriveInfo] when it is missing or doesn't fit. */
        fun parse(text: String?): DriveInfo =
            text
                ?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
                ?: DriveInfo()
    }
}
