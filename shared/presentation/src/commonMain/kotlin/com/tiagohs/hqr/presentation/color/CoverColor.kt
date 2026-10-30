package com.tiagohs.hqr.presentation.color

import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlin.math.abs
import kotlin.math.max

/** The 12 color roles the screens use, as ARGB ints (design.md §2). */
data class ColorRoles(
    val background: Int,
    val surface1: Int,
    val surface2: Int,
    val surface3: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val outlineVariant: Int,
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
)

/**
 * Color taken from the cover (RF-98, ADR-021): the Material quantizer and score rank the cover colors, the HQR rule
 * skips colors within 35° of the error red's hue and colors with chroma below 16, and without a candidate the work
 * keeps the violet base. The roles come from Material tones (primary 40/80, containers 90/30), which keep the text
 * contrast at least 4.5:1 whatever the seed.
 */
object CoverColor {
    const val VIOLET: Int = 0xFF5C51A8.toInt()
    private const val ERROR: Int = 0xFFB3261E.toInt()
    private const val MIN_HUE_DISTANCE = 35.0
    private const val MIN_CHROMA = 16.0
    private const val MAX_COLORS = 128
    private const val RANKED = 6

    private val errorHue = Hct.fromInt(ERROR).hue

    /** Pixels in ARGB (a downscaled cover, ~128 px). Returns the seed color for the work. */
    fun seed(pixels: IntArray): Int {
        if (pixels.isEmpty()) return VIOLET
        val ranked = Score.score(QuantizerCelebi.quantize(pixels, MAX_COLORS), RANKED, VIOLET, true)
        return ranked.firstOrNull { isUsable(it) } ?: VIOLET
    }

    /** The HQR rule: far enough from the error red and colorful enough. */
    fun isUsable(argb: Int): Boolean {
        val hct = Hct.fromInt(argb)
        return hueDistance(hct.hue, errorHue) >= MIN_HUE_DISTANCE && hct.chroma >= MIN_CHROMA
    }

    fun roles(
        seed: Int,
        dark: Boolean,
    ): ColorRoles {
        val hct = Hct.fromInt(seed)
        val primary = TonalPalette.fromHueAndChroma(hct.hue, max(hct.chroma, PRIMARY_MIN_CHROMA))
        val neutral = TonalPalette.fromHueAndChroma(hct.hue, NEUTRAL_CHROMA)
        val neutralVariant = TonalPalette.fromHueAndChroma(hct.hue, NEUTRAL_VARIANT_CHROMA)
        val t = if (dark) DARK_TONES else LIGHT_TONES
        val tones =
            (List(NEUTRAL_ROLES) { neutral } + List(NEUTRAL_VARIANT_ROLES) { neutralVariant } + List(PRIMARY_ROLES) { primary })
                .mapIndexed { i, palette -> palette.tone(t[i]) }
        return ColorRoles(
            background = tones[ROLE_BACKGROUND],
            surface1 = tones[ROLE_SURFACE1],
            surface2 = tones[ROLE_SURFACE2],
            surface3 = tones[ROLE_SURFACE3],
            onSurface = tones[ROLE_ON_SURFACE],
            onSurfaceVariant = tones[ROLE_ON_SURFACE_VARIANT],
            outline = tones[ROLE_OUTLINE],
            outlineVariant = tones[ROLE_OUTLINE_VARIANT],
            primary = tones[ROLE_PRIMARY],
            onPrimary = tones[ROLE_ON_PRIMARY],
            primaryContainer = tones[ROLE_PRIMARY_CONTAINER],
            onPrimaryContainer = tones[ROLE_ON_PRIMARY_CONTAINER],
        )
    }

    private fun hueDistance(
        a: Double,
        b: Double,
    ): Double {
        val d = abs(a - b) % FULL_TURN
        return if (d > HALF_TURN) FULL_TURN - d else d
    }
}

private const val PRIMARY_MIN_CHROMA = 48.0
private const val NEUTRAL_CHROMA = 6.0
private const val NEUTRAL_VARIANT_CHROMA = 10.0
private const val NEUTRAL_ROLES = 5
private const val NEUTRAL_VARIANT_ROLES = 3
private const val PRIMARY_ROLES = 4
private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0

// Tones per role, in the order of ColorRoles: 5 neutral, 3 neutral variant, 4 primary.
private val LIGHT_TONES = intArrayOf(98, 96, 94, 90, 10, 30, 50, 80, 40, 100, 90, 10)
private val DARK_TONES = intArrayOf(6, 10, 17, 22, 90, 80, 60, 30, 80, 20, 30, 90)

private const val ROLE_BACKGROUND = 0
private const val ROLE_SURFACE1 = 1
private const val ROLE_SURFACE2 = 2
private const val ROLE_SURFACE3 = 3
private const val ROLE_ON_SURFACE = 4
private const val ROLE_ON_SURFACE_VARIANT = 5
private const val ROLE_OUTLINE = 6
private const val ROLE_OUTLINE_VARIANT = 7
private const val ROLE_PRIMARY = 8
private const val ROLE_ON_PRIMARY = 9
private const val ROLE_PRIMARY_CONTAINER = 10
private const val ROLE_ON_PRIMARY_CONTAINER = 11

/** Pixels of a work's cover in ARGB, downscaled (~128 px); null when the cover can't be read (RF-98). */
fun interface CoverPixels {
    suspend fun pixels(manga: com.tiagohs.hqr.domain.model.Manga): IntArray?
}
