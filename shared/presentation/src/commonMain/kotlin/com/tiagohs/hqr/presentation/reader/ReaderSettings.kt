package com.tiagohs.hqr.presentation.reader

import com.tiagohs.hqr.domain.model.ReadingKind
import kotlinx.serialization.Serializable

/** Reader modes (RF-36). */
@Serializable
enum class ReaderMode { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON }

/** Tap zone layouts (RF-38). */
@Serializable
enum class TapZones { DEFAULT, KINDLE, EDGES, SIDES, OFF }

/** Page scale (RF-39). */
@Serializable
enum class PageScale { FIT_SCREEN, FIT_WIDTH, FIT_HEIGHT, ORIGINAL }

/** Reader background (RF-46). */
@Serializable
enum class ReaderBackground { BLACK, GRAY, WHITE, AUTO }

@Serializable
data class ReaderSettings(
    val mode: ReaderMode = ReaderMode.RIGHT_TO_LEFT,
    val tapZones: TapZones = TapZones.DEFAULT,
    val invertZones: Boolean = false,
    val scale: PageScale = PageScale.FIT_SCREEN,
    val background: ReaderBackground = ReaderBackground.BLACK,
    val doublePage: Boolean = true,
    val cropBorders: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showPageNumber: Boolean = true,
    val preload: Int = 4,
    val volumeKeys: Boolean = false,
    /** RF-44: in portrait, a wide page shows as two screens. */
    val splitWide: Boolean = false,
    /** RF-44: the other page (or half) first. */
    val invertPages: Boolean = false,
    val webtoonMargin: Int = 0,
) {
    companion object {
        /** RF-37: first read suggestion by kind; local files and western comics read left to right. */
        fun suggestedMode(kind: ReadingKind): ReaderMode =
            when (kind) {
                ReadingKind.MANGA -> ReaderMode.RIGHT_TO_LEFT
                ReadingKind.MANHWA -> ReaderMode.WEBTOON
                ReadingKind.COMIC, ReadingKind.UNKNOWN -> ReaderMode.LEFT_TO_RIGHT
            }
    }
}

/** What a tap in the reader does (RF-38). */
enum class TapAction { PREVIOUS, NEXT, MENU }

private const val THIRD = 1f / 3
private const val TWO_THIRDS = 2f / 3
private const val EDGE = 0.2f
private const val FAR_EDGE = 0.8f

object TapZoneMap {
    /**
     * [x] and [y] are 0..1 inside the screen. Left and right swap for right-to-left reading, or when the user
     * inverted the zones (not both); top and bottom never swap.
     */
    fun action(
        zones: TapZones,
        mode: ReaderMode,
        invert: Boolean,
        x: Float,
        y: Float,
    ): TapAction {
        val mirrored = (mode == ReaderMode.RIGHT_TO_LEFT) != invert
        val left = if (mirrored) TapAction.NEXT else TapAction.PREVIOUS
        val right = if (mirrored) TapAction.PREVIOUS else TapAction.NEXT
        return when (zones) {
            TapZones.OFF -> TapAction.MENU
            TapZones.SIDES -> horizontal(x, left, right)
            TapZones.EDGES ->
                when {
                    x < EDGE || x > FAR_EDGE -> TapAction.NEXT
                    y > FAR_EDGE -> TapAction.PREVIOUS
                    else -> TapAction.MENU
                }
            TapZones.KINDLE ->
                if (y < THIRD) {
                    TapAction.MENU
                } else if (x < THIRD) {
                    left
                } else {
                    right
                }
            TapZones.DEFAULT ->
                when {
                    y < THIRD -> TapAction.PREVIOUS
                    y > TWO_THIRDS -> TapAction.NEXT
                    else -> horizontal(x, left, right)
                }
        }
    }

    private fun horizontal(
        x: Float,
        left: TapAction,
        right: TapAction,
    ) = when {
        x < THIRD -> left
        x > TWO_THIRDS -> right
        else -> TapAction.MENU
    }
}
