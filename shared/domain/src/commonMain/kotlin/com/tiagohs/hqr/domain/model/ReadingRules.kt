package com.tiagohs.hqr.domain.model

import kotlin.math.floor

/** What the main button of the work screen says and where it opens (RF-31). */
sealed interface ReadAction {
    data class Start(
        val chapter: Chapter,
    ) : ReadAction

    data class Continue(
        val chapter: Chapter,
        val page: Int,
    ) : ReadAction

    data class ReadAgain(
        val chapter: Chapter,
    ) : ReadAction

    data object Nothing : ReadAction
}

object ReadingRules {
    /**
     * RF-31 and RF-92: "Ler" when nothing was read; "Continuar cap. N" for the chapter in progress or the first
     * unread one after the last read; "Ler de novo" when all are read. Repeated numbers from different groups count
     * as one chapter.
     */
    fun readAction(chapters: List<Chapter>): ReadAction {
        val ordered = chapters.sortedBy { it.number }.distinctByNumberPreferringProgress()
        val inProgress = ordered.lastOrNull { it.inProgress }
        val lastRead = ordered.indexOfLast { it.read }
        val nextUnread = ordered.drop(lastRead + 1).firstOrNull { !it.read } ?: ordered.firstOrNull { !it.read }
        return when {
            ordered.isEmpty() -> ReadAction.Nothing
            inProgress != null -> ReadAction.Continue(inProgress, inProgress.lastPage)
            lastRead < 0 -> ReadAction.Start(ordered.first())
            nextUnread != null -> ReadAction.Continue(nextUnread, 0)
            else -> ReadAction.ReadAgain(ordered.first())
        }
    }

    /**
     * RF-42: there is a gap between [current] and [next] when the whole part of the next number is more than one
     * above the current one (10 → 12 has a gap; 10 → 10.5 → 11 has none). Returns the missing whole numbers.
     */
    fun missingBetween(
        current: Double,
        next: Double,
    ): List<Int> {
        val from = floor(current).toInt()
        val to = floor(next).toInt()
        return if (to - from > 1) ((from + 1) until to).toList() else emptyList()
    }

    /** RF-30: the next chapter after [current] in reading order, skipping repeated numbers. */
    fun nextChapter(
        chapters: List<Chapter>,
        current: Chapter,
    ): Chapter? =
        chapters
            .sortedBy { it.number }
            .distinctByNumberPreferringProgress()
            .firstOrNull { it.number > current.number }

    fun previousChapter(
        chapters: List<Chapter>,
        current: Chapter,
    ): Chapter? =
        chapters
            .sortedBy { it.number }
            .distinctByNumberPreferringProgress()
            .lastOrNull { it.number < current.number }

    private fun List<Chapter>.distinctByNumberPreferringProgress(): List<Chapter> =
        groupBy { it.number }
            .values
            .map { same -> same.firstOrNull { it.inProgress } ?: same.firstOrNull { it.read } ?: same.first() }
            .sortedBy { it.number }
}
