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
            everything && item.manga.source.status == PublicationStatus.COMPLETED -> ReadingState.FINISHED
            everything -> ReadingState.UP_TO_DATE
            read == 0 && item.lastReadAtEpochMs == null && item.timesRead == 0 -> ReadingState.PENDING
            item.timesRead > 0 && read == 0 -> ReadingState.READING
            item.timesRead > 0 && newSinceLastRead(item) && read > 0 -> ReadingState.NEW_CHAPTERS
            else -> ReadingState.READING
        }
    }

    /** 2 for the second reading and so on; null on the first one, which needs no label. */
    fun readingNumber(item: LibraryItem): Int? = if (item.timesRead > 0 && state(item) == ReadingState.READING) item.timesRead + 1 else null

    fun summary(items: List<LibraryItem>): ProgressSummary {
        val states = items.map(::state)
        val worksRead =
            items.indices.count {
                states[it] == ReadingState.UP_TO_DATE ||
                    states[it] == ReadingState.FINISHED ||
                    items[it].timesRead > 0
            }
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

    /** RF-133: the last work read among [items]; the screen passes the works of the area it shows. */
    fun continueReading(
        latest: List<HistoryEntry>,
        libraryIds: Set<Long>,
        areaIds: Set<Long>,
    ): HistoryEntry? = latest.firstOrNull { !it.chapter.read && it.manga.id in libraryIds && it.manga.id in areaIds }

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

    private const val PERCENT = 100

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
