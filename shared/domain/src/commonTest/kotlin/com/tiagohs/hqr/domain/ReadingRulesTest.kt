package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.ReadingRules
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.chapterLabel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ReadingRulesTest {
    private fun ch(
        number: Double,
        read: Boolean = false,
        page: Int = 0,
        group: String = "A",
    ) = Chapter(
        id = (number * 10).toLong(),
        mangaId = 1,
        source = SourceChapter(key = "$group-$number", number = number, group = group),
        read = read,
        lastPage = page,
        pageCount = 22,
    )

    @Test
    fun RF31_nothingReadSaysReadFromTheFirst() {
        val action = ReadingRules.readAction(listOf(ch(3.0), ch(1.0), ch(2.0)))
        action.shouldBeInstanceOf<ReadAction.Start>().chapter.number shouldBe 1.0
    }

    @Test
    fun RF31_chapterInProgressContinuesOnItsPage() {
        val action = ReadingRules.readAction(listOf(ch(11.0, read = true), ch(12.0, page = 8), ch(13.0)))
        val cont = action.shouldBeInstanceOf<ReadAction.Continue>()
        cont.chapter.number shouldBe 12.0
        cont.page shouldBe 8
    }

    @Test
    fun RF31_afterTheLastReadContinuesOnTheNextUnread() {
        val action = ReadingRules.readAction(listOf(ch(1.0, read = true), ch(2.0, read = true), ch(3.0)))
        action.shouldBeInstanceOf<ReadAction.Continue>().chapter.number shouldBe 3.0
    }

    @Test
    fun RF31_allReadSaysReadAgain() {
        val action = ReadingRules.readAction(listOf(ch(1.0, read = true), ch(2.0, read = true)))
        action.shouldBeInstanceOf<ReadAction.ReadAgain>().chapter.number shouldBe 1.0
    }

    @Test
    fun RF31_noChaptersHasNoAction() {
        ReadingRules.readAction(emptyList()) shouldBe ReadAction.Nothing
    }

    @Test
    fun RF92_repeatedNumbersFromTwoGroupsCountAsOne() {
        val chapters = listOf(ch(5.0, read = true, group = "A"), ch(5.0, group = "B"), ch(6.0))
        ReadingRules
            .readAction(chapters)
            .shouldBeInstanceOf<ReadAction.Continue>()
            .chapter.number shouldBe 6.0
        ReadingRules.nextChapter(chapters, chapters[0])?.number shouldBe 6.0
    }

    @Test
    fun RF42_gapIsWhenTheWholePartJumpsMoreThanOne() {
        ReadingRules.missingBetween(10.0, 12.0) shouldBe listOf(11)
        ReadingRules.missingBetween(10.0, 10.5) shouldBe emptyList()
        ReadingRules.missingBetween(10.5, 11.0) shouldBe emptyList()
        ReadingRules.missingBetween(8.0, 11.0) shouldBe listOf(9, 10)
    }

    @Test
    fun RF30_previousAndNextFollowTheNumbers() {
        val chapters = listOf(ch(1.0), ch(2.0), ch(2.5), ch(3.0))
        ReadingRules.nextChapter(chapters, chapters[1])?.number shouldBe 2.5
        ReadingRules.previousChapter(chapters, chapters[1])?.number shouldBe 1.0
        ReadingRules.nextChapter(chapters, chapters[3]) shouldBe null
    }

    @Test
    fun chapterLabelDropsTheZeroDecimal() {
        12.0.chapterLabel() shouldBe "12"
        10.5.chapterLabel() shouldBe "10.5"
    }
}
