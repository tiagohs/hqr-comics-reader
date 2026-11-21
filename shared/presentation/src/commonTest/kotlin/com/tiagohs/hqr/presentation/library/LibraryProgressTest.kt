package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryProgressTest {
    @Suppress("LongParameterList")
    private fun item(
        id: Long,
        title: String = "Obra $id",
        unread: Int = 0,
        total: Int = 10,
        lastRead: Long? = null,
        latest: Long? = null,
        times: Int = 0,
        status: PublicationStatus = PublicationStatus.ONGOING,
        genres: List<String> = emptyList(),
    ) = LibraryItem(
        Manga(id, SourceManga(SourceId.MANGADEX, "k$id", title, status = status, genres = genres), true, id),
        unread,
        0,
        emptySet(),
        lastRead,
        latest,
        total,
        times,
    )

    @Test
    fun RF132_a_work_with_nothing_read_is_pending_never_up_to_date() {
        assertEquals(ReadingState.PENDING, LibraryProgress.state(item(1, unread = 10)))
    }

    @Test
    fun RF132_some_chapters_read_is_reading() {
        assertEquals(ReadingState.READING, LibraryProgress.state(item(1, unread = 4, lastRead = 5)))
    }

    @Test
    fun RF132_everything_read_is_up_to_date_while_the_source_publishes_and_finished_when_it_is_done() {
        assertEquals(ReadingState.UP_TO_DATE, LibraryProgress.state(item(1, unread = 0, lastRead = 5)))
        assertEquals(
            ReadingState.FINISHED,
            LibraryProgress.state(item(1, unread = 0, lastRead = 5, status = PublicationStatus.COMPLETED)),
        )
    }

    @Test
    fun RF132_chapters_that_came_out_after_a_full_reading_are_new_chapters() {
        val state = LibraryProgress.state(item(1, unread = 3, lastRead = 5, latest = 9, times = 1))
        assertEquals(ReadingState.NEW_CHAPTERS, state)
    }

    @Test
    fun RF132_a_work_never_finished_with_unread_chapters_stays_reading_even_if_the_source_is_newer() {
        assertEquals(ReadingState.READING, LibraryProgress.state(item(1, unread = 3, lastRead = 5, latest = 9)))
    }

    @Test
    fun RF135_reading_again_counts_from_the_second_reading_and_keeps_the_chapters_read() {
        val again = item(1, unread = 4, lastRead = 5, times = 1)
        assertEquals(2, LibraryProgress.readingNumber(again))
        assertEquals(6, LibraryProgress.chaptersRead(again))
        assertNull(LibraryProgress.readingNumber(item(2, unread = 4, lastRead = 5)))
        assertEquals(3, LibraryProgress.readingNumber(item(3, unread = 4, lastRead = 5, times = 2)))
    }

    @Test
    fun RF132_a_work_read_once_counts_as_100_percent_even_when_it_is_being_read_again() {
        val summary =
            LibraryProgress.summary(
                listOf(
                    item(1, unread = 0, lastRead = 1),
                    item(2, unread = 6, lastRead = 2, times = 1),
                    item(3, unread = 10),
                    item(4, unread = 5, lastRead = 3),
                ),
            )
        assertEquals(2, summary.worksRead)
        assertEquals(50, summary.percent)
        assertEquals(40, summary.chaptersTotal)
        assertEquals(19, summary.chaptersRead)
        assertEquals(1, summary.byState[ReadingState.PENDING])
    }

    @Test
    fun RF132_an_empty_library_is_zero_percent() {
        assertEquals(0, LibraryProgress.summary(emptyList()).percent)
    }

    @Test
    fun RF134_characters_come_from_the_title_and_the_genres() {
        val batman = item(1, title = "Batman: Ano Um")
        val spider = item(2, title = "Ultimate", genres = listOf("Spider-Man"))
        assertEquals(setOf("Batman"), LibraryProgress.characters(batman))
        assertEquals(setOf("Homem-Aranha"), LibraryProgress.characters(spider))
        assertEquals(emptySet(), LibraryProgress.characters(item(3, title = "Akira")))
    }
}
