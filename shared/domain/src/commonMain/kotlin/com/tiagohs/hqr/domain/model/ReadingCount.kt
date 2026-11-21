package com.tiagohs.hqr.domain.model

/** RF-135: how many readings of a work ended and where a reading again is. */
data class ReadingCount(
    val timesRead: Int,
    val rereadChapter: Double?,
) {
    /** "Ler de novo" from [chapterNumber]: the first full reading counts as one if it was never counted. */
    fun start(chapterNumber: Double): ReadingCount = ReadingCount(maxOf(timesRead, 1), chapterNumber)

    /**
     * After the reader saved [opened]: a reading again moves to it (to the next one once [finished]) and ends on the
     * last chapter; otherwise the first
     * time every chapter is read counts one reading, so new chapters later show as "Saiu capítulo".
     */
    fun after(
        chapters: List<Chapter>,
        opened: Chapter?,
        finished: Boolean,
    ): ReadingCount {
        if (rereadChapter != null) {
            val chapter = opened ?: return this
            val next = chapters.map { it.number }.filter { it > chapter.number }.minOrNull()
            return when {
                !finished -> copy(rereadChapter = chapter.number)
                next == null -> ReadingCount(timesRead + 1, null)
                else -> copy(rereadChapter = next)
            }
        }
        val all = chapters.isNotEmpty() && chapters.all { it.read }
        return if (all && timesRead == 0) ReadingCount(1, null) else this
    }

    companion object {
        fun of(manga: Manga): ReadingCount = ReadingCount(manga.timesRead, manga.rereadChapter)
    }
}
