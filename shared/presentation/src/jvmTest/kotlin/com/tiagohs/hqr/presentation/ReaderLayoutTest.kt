package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.presentation.reader.BorderCrop
import com.tiagohs.hqr.presentation.reader.KeyAction
import com.tiagohs.hqr.presentation.reader.PageView
import com.tiagohs.hqr.presentation.reader.ReaderKey
import com.tiagohs.hqr.presentation.reader.ReaderKeys
import com.tiagohs.hqr.presentation.reader.ReaderLayout
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** The reader's page layout, margins and keys, without a screen (RF-37, RF-44, RF-45, RF-47). */
class ReaderLayoutTest {
    @Test
    fun RF44_doublePagePairsAfterTheCoverAndKeepsWidePagesAlone() {
        val views = ReaderLayout.views(count = 8, wide = setOf(4), double = true, split = false)
        views shouldBe
            listOf(
                PageView.Single(0),
                // Pages 2 and 3 (indexes 1 and 2) together.
                PageView.Spread(1, 2),
                // The page before a wide one can't pair with it.
                PageView.Single(3),
                PageView.Single(4),
                PageView.Spread(5, 6),
                PageView.Single(7),
            )
        ReaderLayout.viewOf(views, 2) shouldBe 1
        ReaderLayout.viewOf(views, 99) shouldBe views.lastIndex
    }

    @Test
    fun RF44_portraitSplitsWidePagesAndOrderFollowsTheReading() {
        ReaderLayout.views(count = 3, wide = setOf(1), double = false, split = true) shouldBe
            listOf(PageView.Single(0), PageView.Half(1, 0), PageView.Half(1, 1), PageView.Single(2))
        ReaderLayout.views(count = 2, wide = emptySet(), double = false, split = false) shouldBe
            listOf(PageView.Single(0), PageView.Single(1))
        ReaderLayout.firstOnLeft(ReaderMode.LEFT_TO_RIGHT, invert = false) shouldBe true
        ReaderLayout.firstOnLeft(ReaderMode.RIGHT_TO_LEFT, invert = false) shouldBe false
        ReaderLayout.firstOnLeft(ReaderMode.RIGHT_TO_LEFT, invert = true) shouldBe true
    }

    private fun page(
        width: Int,
        height: Int,
        margin: Int,
        border: Int,
        ink: Int,
    ): (Int, Int) -> Int =
        { x, y ->
            val inside = x in margin until width - margin && y in margin until height - margin
            if (inside) ink else border
        }

    @Test
    fun RF45_uniformWhiteOrBlackMarginsAreCut() {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val ink = 0xFF4A6FA5.toInt()
        val area = BorderCrop.area(400, 600, page(400, 600, 40, white, ink))!!
        // The margin goes (to the sampling step) and the drawing stays whole.
        (area.left in 37..40) shouldBe true
        (area.top in 37..40) shouldBe true
        (area.right in 360..363) shouldBe true
        (area.bottom in 560..563) shouldBe true
        (BorderCrop.area(400, 600, page(400, 600, 30, black, ink)) != null) shouldBe true
        // No margin, or a colored one: nothing is cut.
        BorderCrop.area(400, 600) { _, _ -> ink } shouldBe null
        BorderCrop.area(400, 600, page(400, 600, 40, 0xFFCC0000.toInt(), ink)) shouldBe null
        // A page that is almost all margin is left alone.
        BorderCrop.area(400, 600, page(400, 600, 150, white, ink)) shouldBe null
    }

    @Test
    fun RF47_volumeKeysOnlyWhenOnAndControllerShoulders() {
        ReaderKeys.action(ReaderKey.VOLUME_DOWN, ReaderMode.RIGHT_TO_LEFT, volumeKeys = false) shouldBe null
        ReaderKeys.action(ReaderKey.VOLUME_DOWN, ReaderMode.RIGHT_TO_LEFT, volumeKeys = true) shouldBe KeyAction.NEXT
        ReaderKeys.action(ReaderKey.VOLUME_UP, ReaderMode.LEFT_TO_RIGHT, volumeKeys = true) shouldBe KeyAction.PREVIOUS
        ReaderKeys.action(ReaderKey.NEXT_BUTTON, ReaderMode.LEFT_TO_RIGHT) shouldBe KeyAction.NEXT
        ReaderKeys.action(ReaderKey.PREVIOUS_BUTTON, ReaderMode.LEFT_TO_RIGHT) shouldBe KeyAction.PREVIOUS
        // The right arrow goes forward left to right.
        ReaderKeys.action(ReaderKey.RIGHT, ReaderMode.LEFT_TO_RIGHT) shouldBe KeyAction.NEXT
    }

    @Test
    fun RF37_firstReadModeByKind() {
        ReaderSettings.suggestedMode(ReadingKind.MANGA) shouldBe ReaderMode.RIGHT_TO_LEFT
        ReaderSettings.suggestedMode(ReadingKind.MANHWA) shouldBe ReaderMode.WEBTOON
        ReaderSettings.suggestedMode(ReadingKind.COMIC) shouldBe ReaderMode.LEFT_TO_RIGHT
    }
}
