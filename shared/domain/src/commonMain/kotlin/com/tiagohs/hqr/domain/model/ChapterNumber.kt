package com.tiagohs.hqr.domain.model

/** Formats a chapter number the way people write it: 12, 10.5 (never 12.0). */
fun Double.chapterLabel(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/**
 * Chapters with no number (an annual, a special) get one each: -1 for the oldest, -2 for the next, counting from the
 * end of a newest-first list, so they stay apart in the reading order and keep their place when new ones arrive. Below
 * zero, a migration never matches them (RF-69).
 */
fun List<SourceChapter>.numberUnnumbered(): List<SourceChapter> {
    var next = 0
    return asReversed()
        .map { if (it.number < 0) it.copy(number = -(++next).toDouble()) else it }
        .asReversed()
}
