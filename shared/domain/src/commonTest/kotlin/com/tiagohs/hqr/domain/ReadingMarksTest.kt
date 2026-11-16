package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.ReadingMark
import com.tiagohs.hqr.domain.model.ReadingMarks
import com.tiagohs.hqr.domain.model.Shelf
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ReadingMarksTest {
    private fun chapter(
        id: Long,
        number: Double,
        read: Boolean = false,
        page: Int = 0,
    ) = Chapter(id, 1, SourceChapter("c$id", number), read = read, lastPage = page, pageCount = null)

    // The source lists the newest first; two groups translated chapter 2.
    private val chapters = listOf(chapter(5, 4.0), chapter(4, 3.0), chapter(3, 2.0), chapter(6, 2.0), chapter(2, 1.5), chapter(1, 1.0))

    @Test
    fun RF126_up_to_a_chapter_reads_it_and_every_one_before_and_unreads_the_rest() {
        val plan = ReadingMarks.plan(chapters, ReadingMark.UpTo(3))
        plan.read.toSet() shouldBe setOf(1L, 2L, 3L, 6L)
        plan.unread.toSet() shouldBe setOf(4L, 5L)
    }

    @Test
    fun RF126_none_and_all_cover_every_chapter() {
        ReadingMarks.plan(chapters, ReadingMark.None).unread.size shouldBe 6
        ReadingMarks.plan(chapters, ReadingMark.None).read shouldBe emptyList()
        ReadingMarks.plan(chapters, ReadingMark.All).read.size shouldBe 6
    }

    @Test
    fun RF126_the_sheet_opens_on_what_is_already_marked() {
        ReadingMarks.current(chapters) shouldBe ReadingMark.None
        ReadingMarks.current(chapters.map { it.copy(read = true) }) shouldBe ReadingMark.All
        val upToThree = chapters.map { if (it.number <= 3.0) it.copy(read = true) else it }
        ReadingMarks.current(upToThree) shouldBe ReadingMark.UpTo(4)
        // A chapter started but not finished counts as the point reached.
        ReadingMarks.current(chapters.map { if (it.id == 2L) it.copy(lastPage = 5) else it }) shouldBe ReadingMark.UpTo(2)
    }

    @Test
    fun RF126_the_slider_walks_the_chapters_in_reading_order_once_per_number() {
        ReadingMarks.ordered(chapters).map { it.id } shouldBe listOf(1L, 2L, 3L, 4L, 5L)
    }

    @Test
    fun RF128_a_work_follows_its_source_unless_it_has_its_own_shelf() {
        Shelf.of(null, SourceId.MANGADEX, emptyMap()) shouldBe Shelf.MANGA
        Shelf.of(null, SourceId.HQ_NOW, emptyMap()) shouldBe Shelf.COMICS
        Shelf.of(null, SourceId.LOCAL, emptyMap()) shouldBe Shelf.COMICS
        Shelf.of(null, SourceId.MANGADEX, mapOf("mangadex" to Shelf.COMICS)) shouldBe Shelf.COMICS
        Shelf.of(Shelf.MANGA, SourceId.HQ_NOW, mapOf("hqnow" to Shelf.COMICS)) shouldBe Shelf.MANGA
    }

    @Test
    fun RF128_the_source_shelves_survive_a_round_trip_through_the_settings() {
        val map = mapOf("mangadex" to Shelf.COMICS, "hqnow" to Shelf.MANGA)
        Shelf.decode(Shelf.encode(map)) shouldBe map
        Shelf.decode("garbage,x=NOPE,=MANGA") shouldBe emptyMap()
        Shelf.decode(null) shouldBe emptyMap()
    }
}
