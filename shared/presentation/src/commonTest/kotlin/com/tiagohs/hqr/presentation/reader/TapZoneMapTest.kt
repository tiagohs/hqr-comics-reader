package com.tiagohs.hqr.presentation.reader

import com.tiagohs.hqr.presentation.reader.TapAction.MENU
import com.tiagohs.hqr.presentation.reader.TapAction.NEXT
import com.tiagohs.hqr.presentation.reader.TapAction.PREVIOUS
import kotlin.test.Test
import kotlin.test.assertEquals

class TapZoneMapTest {
    private fun tap(
        zones: TapZones,
        mode: ReaderMode,
        x: Float,
        y: Float,
        invert: Boolean = false,
    ) = TapZoneMap.action(zones, mode, invert, x, y)

    @Test
    fun RF38_defaultZonesLeftToRight() {
        assertEquals(PREVIOUS, tap(TapZones.DEFAULT, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.5f))
        assertEquals(NEXT, tap(TapZones.DEFAULT, ReaderMode.LEFT_TO_RIGHT, 0.9f, 0.5f))
        assertEquals(MENU, tap(TapZones.DEFAULT, ReaderMode.LEFT_TO_RIGHT, 0.5f, 0.5f))
        assertEquals(PREVIOUS, tap(TapZones.DEFAULT, ReaderMode.LEFT_TO_RIGHT, 0.9f, 0.1f))
        assertEquals(NEXT, tap(TapZones.DEFAULT, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.9f))
    }

    @Test
    fun RF38_rightToLeftSwapsSidesButNotTopAndBottom() {
        assertEquals(NEXT, tap(TapZones.DEFAULT, ReaderMode.RIGHT_TO_LEFT, 0.1f, 0.5f))
        assertEquals(PREVIOUS, tap(TapZones.DEFAULT, ReaderMode.RIGHT_TO_LEFT, 0.9f, 0.5f))
        assertEquals(PREVIOUS, tap(TapZones.DEFAULT, ReaderMode.RIGHT_TO_LEFT, 0.5f, 0.1f))
        assertEquals(NEXT, tap(TapZones.DEFAULT, ReaderMode.RIGHT_TO_LEFT, 0.5f, 0.9f))
    }

    @Test
    fun RF38_invertUndoesTheRightToLeftSwap() {
        assertEquals(PREVIOUS, tap(TapZones.SIDES, ReaderMode.RIGHT_TO_LEFT, 0.1f, 0.5f, invert = true))
        assertEquals(NEXT, tap(TapZones.SIDES, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.5f, invert = true))
    }

    @Test
    fun RF38_otherLayouts() {
        assertEquals(MENU, tap(TapZones.OFF, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.5f))
        assertEquals(MENU, tap(TapZones.KINDLE, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.1f))
        assertEquals(PREVIOUS, tap(TapZones.KINDLE, ReaderMode.LEFT_TO_RIGHT, 0.1f, 0.5f))
        assertEquals(NEXT, tap(TapZones.KINDLE, ReaderMode.LEFT_TO_RIGHT, 0.6f, 0.5f))
        assertEquals(NEXT, tap(TapZones.EDGES, ReaderMode.RIGHT_TO_LEFT, 0.05f, 0.5f))
        assertEquals(PREVIOUS, tap(TapZones.EDGES, ReaderMode.LEFT_TO_RIGHT, 0.5f, 0.9f))
        assertEquals(MENU, tap(TapZones.EDGES, ReaderMode.LEFT_TO_RIGHT, 0.5f, 0.5f))
    }
}
