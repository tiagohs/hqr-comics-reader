package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.ReadingCount
import com.tiagohs.hqr.domain.model.SourceChapter
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingCountTest {
    private fun chapter(
        number: Double,
        read: Boolean = true,
    ) = Chapter(number.toLong(), 1, SourceChapter("c$number", number), read = read, lastPage = 0, pageCount = null)

    private val all = (1..12).map { chapter(it.toDouble()) }

    @Test
    fun RF135_the_first_full_reading_counts_one() {
        assertEquals(ReadingCount(1, null), ReadingCount(0, null).after(all, all.last(), finished = true))
        val half = all.mapIndexed { i, c -> if (i > 5) c.copy(read = false) else c }
        assertEquals(ReadingCount(0, null), ReadingCount(0, null).after(half, half[5], finished = true))
    }

    @Test
    fun RF135_read_again_moves_with_the_last_chapter_opened_and_ends_on_the_last_one() {
        val again = ReadingCount(0, null).start(1.0)
        assertEquals(ReadingCount(1, 1.0), again)
        val second = again.after(all, all[1], finished = false)
        assertEquals(ReadingCount(1, 2.0), second)
        assertEquals(ReadingCount(1, 3.0), second.after(all, all[1], finished = true))
        assertEquals(ReadingCount(2, null), second.after(all, all.last(), finished = true))
    }

    @Test
    fun RF135_the_third_reading_starts_from_two_finished_ones() {
        assertEquals(ReadingCount(2, 3.0), ReadingCount(2, null).start(3.0))
    }
}
