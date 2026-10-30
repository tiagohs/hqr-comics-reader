package com.tiagohs.hqr.presentation

import com.materialkolor.hct.Hct
import com.tiagohs.hqr.presentation.color.CoverColor
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.pow
import kotlin.test.Test

class CoverColorTest {
    private fun hex(argb: Int) = "#%06x".format(argb and 0xFFFFFF)

    private fun cover(vararg parts: Pair<Int, Int>): IntArray = parts.flatMap { (color, count) -> List(count) { color } }.toIntArray()

    private val red = 0xFFFC2301.toInt()
    private val yellow = 0xFFFCF543.toInt()
    private val gray = 0xFF6F7277.toInt()

    @Test
    fun RF98_violetSeedGivesTheBaseScheme() {
        // The same values as docs/tokens.json and the canvas (violet base).
        val light = CoverColor.roles(CoverColor.VIOLET, dark = false)
        hex(light.primary) shouldBe "#5d52a9"
        hex(light.background) shouldBe "#fcf8ff"
        val dark = CoverColor.roles(CoverColor.VIOLET, dark = true)
        hex(dark.primary) shouldBe "#c7bfff"
        hex(dark.background) shouldBe "#141318"
    }

    @Test
    fun RF98_yellowSeedMatchesTheCanvas() {
        hex(CoverColor.roles(yellow, dark = false).primary) shouldBe "#646100"
        hex(CoverColor.roles(yellow, dark = true).primary) shouldBe "#d2cc0d"
    }

    @Test
    fun RF98_redDominantCoverSkipsToTheNextUsableColor() {
        val seed = CoverColor.seed(cover(red to 6000, yellow to 2500, gray to 1500))
        CoverColor.isUsable(seed) shouldBe true
        Hct.fromInt(seed).hue shouldBeGreaterThan 90.0
        Hct.fromInt(seed).hue shouldBeLessThan 120.0
    }

    @Test
    fun RF98_redAndDullColorsFallBackToViolet() {
        CoverColor.seed(cover(red to 7000, gray to 3000)) shouldBe CoverColor.VIOLET
        CoverColor.seed(IntArray(0)) shouldBe CoverColor.VIOLET
    }

    @Test
    fun RF98_textOnPrimaryKeepsContrastForAnySeed() {
        listOf(yellow, red, gray, 0xFF6DABAA.toInt(), 0xFF0590CA.toInt(), CoverColor.VIOLET).forEach { seed ->
            for (dark in listOf(false, true)) {
                val roles = CoverColor.roles(seed, dark)
                contrast(roles.onPrimary, roles.primary) shouldBeGreaterThan 4.5
                contrast(roles.onSurface, roles.background) shouldBeGreaterThan 4.5
                contrast(roles.onPrimaryContainer, roles.primaryContainer) shouldBeGreaterThan 4.5
            }
        }
    }

    private fun contrast(
        a: Int,
        b: Int,
    ): Double {
        fun lum(c: Int): Double {
            fun ch(v: Int): Double {
                val s = v / 255.0
                return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * ch((c shr 16) and 255) + 0.7152 * ch((c shr 8) and 255) + 0.0722 * ch(c and 255)
        }
        val (hi, lo) = listOf(lum(a), lum(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}
