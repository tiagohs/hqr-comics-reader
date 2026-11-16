package com.tiagohs.hqr.domain.model

import kotlinx.serialization.Serializable

/** RF-128: the library area of a work besides "Tudo". */
@Serializable
enum class Shelf {
    COMICS,
    MANGA,
    ;

    companion object {
        /** Sources whose works are mangás out of the box; every other source (and files, servers) is HQs. */
        val MANGA_SOURCES = setOf("mangadex", "namicomi", "yomu", "weebcentral", "mangaflix")

        /** Sources that say the area of each work themselves (the Drive's `mangas` and `hqs` folders, RF-130). */
        val KIND_SOURCES = setOf("drive")

        fun sourceDefault(sourceId: SourceId): Shelf = if (sourceId.value in MANGA_SOURCES) MANGA else COMICS

        /** The work's own choice, else the kind a [KIND_SOURCES] source gave it, else the source's, else the factory default. */
        fun of(
            own: Shelf?,
            sourceId: SourceId,
            sourceShelves: Map<String, Shelf>,
            kind: ReadingKind = ReadingKind.UNKNOWN,
        ): Shelf = own ?: byKind(sourceId, kind) ?: sourceShelves[sourceId.value] ?: sourceDefault(sourceId)

        private fun byKind(
            sourceId: SourceId,
            kind: ReadingKind,
        ): Shelf? =
            when {
                sourceId.value !in KIND_SOURCES -> null
                kind == ReadingKind.MANGA || kind == ReadingKind.MANHWA -> MANGA
                kind == ReadingKind.COMIC -> COMICS
                else -> null
            }

        /** "mangadex=COMICS,hqnow=MANGA": one preference that follows the account. */
        fun encode(map: Map<String, Shelf>): String = map.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value.name}" }

        fun decode(text: String?): Map<String, Shelf> =
            text
                .orEmpty()
                .split(',')
                .mapNotNull { part ->
                    val (id, name) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                    val shelf = entries.firstOrNull { it.name == name } ?: return@mapNotNull null
                    id.takeIf { it.isNotBlank() }?.let { it to shelf }
                }.toMap()
    }
}

/** RF-126: what the user says they have read of a work, without opening the reader. */
sealed interface ReadingMark {
    data object None : ReadingMark

    data object All : ReadingMark

    /** This chapter and every one numbered before it are read; the rest are not. */
    data class UpTo(
        val chapterId: Long,
    ) : ReadingMark
}

/** A chapter's reading before a mark, so "Desfazer" can put it back. */
data class ChapterReadState(
    val chapterId: Long,
    val read: Boolean,
    val page: Int,
    val pageCount: Int?,
)

data class ReadingPlan(
    val read: List<Long>,
    val unread: List<Long>,
)

object ReadingMarks {
    /** One chapter per number, in reading order: the steps of the slider. */
    fun ordered(chapters: List<Chapter>): List<Chapter> = chapters.sortedWith(compareBy({ it.number }, { it.id })).distinctBy { it.number }

    fun plan(
        chapters: List<Chapter>,
        mark: ReadingMark,
    ): ReadingPlan =
        when (mark) {
            ReadingMark.None -> ReadingPlan(emptyList(), chapters.map { it.id })
            ReadingMark.All -> ReadingPlan(chapters.map { it.id }, emptyList())
            is ReadingMark.UpTo -> {
                val limit = chapters.firstOrNull { it.id == mark.chapterId }?.number ?: Double.NEGATIVE_INFINITY
                val (read, unread) = chapters.partition { it.number <= limit }
                ReadingPlan(read.map { it.id }, unread.map { it.id })
            }
        }

    /** What the sheet shows first: nothing, everything, or the furthest chapter read or started. */
    fun current(chapters: List<Chapter>): ReadingMark {
        if (chapters.isNotEmpty() && chapters.all { it.read }) return ReadingMark.All
        val reached = chapters.filter { it.read || it.inProgress }.maxByOrNull { it.number } ?: return ReadingMark.None
        return ReadingMark.UpTo(ordered(chapters).first { it.number == reached.number }.id)
    }
}
