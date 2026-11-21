package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem

/** RF-132: where the user is in one work, apart from what the source says about it. */
enum class ReadingState {
    /** Nothing read yet. */
    PENDING,

    /** Some chapters read, some not. */
    READING,

    /** Everything read before, and the source added chapters since. */
    NEW_CHAPTERS,

    /** Everything read and the source is still publishing. */
    UP_TO_DATE,

    /** Everything read and the source is done. */
    FINISHED,
}

/** The tracking numbers of a set of works (RF-132). */
data class ProgressSummary(
    val works: Int,
    val worksRead: Int,
    val chaptersRead: Int,
    val chaptersTotal: Int,
    /** Whole percent of the works read, counting a once-read work as 100% even while it is read again. */
    val percent: Int,
    val byState: Map<ReadingState, Int>,
)

object LibraryProgress {
    fun chaptersRead(item: LibraryItem): Int = (item.totalChapters - item.unread).coerceAtLeast(0)

    fun state(item: LibraryItem): ReadingState {
        val read = chaptersRead(item)
        val everything = item.totalChapters > 0 && item.unread == 0
        return when {
            item.manga.isRereading -> ReadingState.READING
            everything && item.manga.source.status == PublicationStatus.COMPLETED -> ReadingState.FINISHED
            everything -> ReadingState.UP_TO_DATE
            read == 0 && !item.inProgress -> ReadingState.PENDING
            item.manga.timesRead > 0 && newSinceLastRead(item) -> ReadingState.NEW_CHAPTERS
            else -> ReadingState.READING
        }
    }

    /** RF-135: 2 for the second reading and so on; null on the first one, which needs no label. */
    fun readingNumber(item: LibraryItem): Int? = if (item.manga.isRereading) item.manga.timesRead + 1 else null

    /** RF-135: "Lida 2×" once more than one reading ended; null for a single reading. */
    fun timesFinished(item: LibraryItem): Int? = item.manga.timesRead.takeIf { it > 1 }

    fun summary(items: List<LibraryItem>): ProgressSummary {
        val states = items.map(::state)
        val worksRead =
            items.indices.count { states[it] in DONE || items[it].manga.timesRead > 0 }
        val total = items.sumOf { it.totalChapters }
        return ProgressSummary(
            works = items.size,
            worksRead = worksRead,
            chaptersRead = items.sumOf(::chaptersRead),
            chaptersTotal = total,
            percent = if (items.isEmpty()) 0 else worksRead * PERCENT / items.size,
            byState = ReadingState.entries.associateWith { s -> states.count { it == s } }.filterValues { it > 0 },
        )
    }

    /**
     * RF-133: the last readings among the works of the area on show, most recent first; a chapter read to the end
     * only counts while its work is read again (RF-135).
     */
    fun continueReading(
        latest: List<HistoryEntry>,
        areaIds: Set<Long>,
        limit: Int = CONTINUE_LIMIT,
    ): List<HistoryEntry> =
        latest
            .filter { (!it.chapter.read || it.manga.isRereading) && it.manga.id in areaIds }
            .sortedByDescending { it.readAtEpochMs }
            .take(limit)

    /** RF-134: the characters of [catalog] a work is about, from its title and genres. */
    fun characters(
        item: LibraryItem,
        catalog: Map<String, List<String>> = HERO_CATALOG,
    ): Set<String> {
        val text = (listOf(item.manga.title) + item.manga.source.genres).joinToString(" ").lowercase()
        return catalog.filterValues { names -> names.any { text.contains(it) } }.keys
    }

    private fun newSinceLastRead(item: LibraryItem): Boolean {
        val latest = item.latestChapterAtEpochMs ?: return false
        return latest > (item.lastReadAtEpochMs ?: 0L)
    }

    private val LibraryItem.inProgress: Boolean get() = lastReadAtEpochMs != null

    private val DONE = setOf(ReadingState.UP_TO_DATE, ReadingState.FINISHED)

    private const val PERCENT = 100
    private const val CONTINUE_LIMIT = 5

    /** Name shown to the user → lower-case words that point to it; the user can edit this later. */
    val HERO_CATALOG: Map<String, List<String>> =
        mapOf(
            "Batman" to listOf("batman", "bat-man", "morcego"),
            "Superman" to listOf("superman", "super-homem"),
            "Homem-Aranha" to listOf("homem-aranha", "spider-man", "homem aranha", "spiderman"),
            "Mulher-Maravilha" to listOf("mulher-maravilha", "wonder woman"),
            "Vingadores" to listOf("vingadores", "avengers"),
            "X-Men" to listOf("x-men", "xmen"),
            "Liga da Justiça" to listOf("liga da justiça", "justice league"),
            "Turma da Mônica" to listOf("turma da mônica", "mônica"),
        )
}
