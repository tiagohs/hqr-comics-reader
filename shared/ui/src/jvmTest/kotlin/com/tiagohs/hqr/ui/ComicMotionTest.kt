package com.tiagohs.hqr.ui

import com.tiagohs.hqr.ui.components.ComicMotion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-118: the comic motion explains and stays short; "reduzir movimento" turns it into a plain fade. */
class ComicMotionTest {
    @Test
    fun RF118_everyMovementFitsBetween150And480Ms() {
        listOf(ComicMotion.PANEL_MS, ComicMotion.STAMP_MS, ComicMotion.CASCADE_MS, ComicMotion.SKETCH_MS).forEach {
            assertTrue(it in 150..480, "$it ms")
        }
        assertTrue(ComicMotion.writeMs(7) <= 480)
    }

    @Test
    fun RF118_aListCascadesOnlyOverItsFirstRows() {
        assertEquals(0, ComicMotion.cascadeDelay(0, reduce = false))
        assertEquals(ComicMotion.cascadeDelay(ComicMotion.CASCADE_ROWS, reduce = false), ComicMotion.cascadeDelay(40, reduce = false))
        assertTrue(ComicMotion.cascadeDelay(1, reduce = false) > 0)
    }

    @Test
    fun RF118_reduceMotionLeavesOnlyTheFade() {
        assertEquals(0, ComicMotion.cascadeDelay(5, reduce = true))
        assertEquals(ComicMotion.FADE_MS, ComicMotion.duration(ComicMotion.PANEL_MS, reduce = true))
        assertEquals(ComicMotion.PANEL_MS, ComicMotion.duration(ComicMotion.PANEL_MS, reduce = false))
        assertEquals(1f, ComicMotion.letterAlpha(index = 6, count = 7, progress = 0f, reduce = true))
    }

    @Test
    fun RF118_theVerticalTitleWritesFromTopToBottom() {
        val halfway = 0.5f
        assertTrue(ComicMotion.letterAlpha(0, 6, halfway, reduce = false) > ComicMotion.letterAlpha(5, 6, halfway, reduce = false))
        assertEquals(0f, ComicMotion.letterAlpha(5, 6, 0f, reduce = false))
        assertEquals(1f, ComicMotion.letterAlpha(5, 6, 1f, reduce = false))
    }
}
