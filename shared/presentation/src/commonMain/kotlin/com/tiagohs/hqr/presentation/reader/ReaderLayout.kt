package com.tiagohs.hqr.presentation.reader

/** What one screen of the paged reader shows (RF-44). */
sealed interface PageView {
    /** The pages on screen, in reading order. */
    val pages: List<Int>

    data class Single(
        val index: Int,
    ) : PageView {
        override val pages get() = listOf(index)
    }

    /** Two pages side by side; [first] comes first in reading order. */
    data class Spread(
        val first: Int,
        val second: Int,
    ) : PageView {
        override val pages get() = listOf(first, second)
    }

    /** One half of a wide page split for a portrait screen; [part] 0 is read first. */
    data class Half(
        val index: Int,
        val part: Int,
    ) : PageView {
        override val pages get() = listOf(index)
    }
}

/**
 * RF-44: how pages become screens. Double page (landscape, tablet, open foldable): the cover alone, then pairs; a wide
 * page (already a spread) stays alone, and so does a page before it. Split (portrait): each wide page becomes two
 * screens. Without either, one page per screen.
 */
object ReaderLayout {
    fun views(
        count: Int,
        wide: Set<Int>,
        double: Boolean,
        split: Boolean,
    ): List<PageView> =
        when {
            double -> spreads(count, wide)
            split ->
                (0 until count).flatMap {
                    if (it in
                        wide
                    ) {
                        listOf(PageView.Half(it, 0), PageView.Half(it, 1))
                    } else {
                        listOf(PageView.Single(it))
                    }
                }
            else -> (0 until count).map { PageView.Single(it) }
        }

    private fun spreads(
        count: Int,
        wide: Set<Int>,
    ): List<PageView> {
        val out = mutableListOf<PageView>()
        var i = 0
        while (i < count) {
            val pairs = i > 0 && i + 1 < count && i !in wide && i + 1 !in wide
            if (pairs) {
                out += PageView.Spread(i, i + 1)
                i += 2
            } else {
                out += PageView.Single(i)
                i++
            }
        }
        return out
    }

    /** The screen showing [page]; the last one when the page is past the end. */
    fun viewOf(
        views: List<PageView>,
        page: Int,
    ): Int = views.indexOfFirst { page in it.pages }.takeIf { it >= 0 } ?: (views.size - 1).coerceAtLeast(0)

    /**
     * RF-44: which side the first page of a spread (or the first half of a wide page) goes. Right-to-left reading puts
     * it on the right; "inverter a ordem" swaps that.
     */
    fun firstOnLeft(
        mode: ReaderMode,
        invert: Boolean,
    ): Boolean = (mode != ReaderMode.RIGHT_TO_LEFT) != invert
}

/**
 * RF-45: the area of a page inside a uniform white or black margin. [pixel] answers ARGB at (x, y). Null when there is
 * no margin worth cutting (under 2 % of a side) or when cutting would leave less than half the page.
 */
object BorderCrop {
    data class Area(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width get() = right - left
        val height get() = bottom - top
    }

    private const val LIGHT = 235
    private const val DARK = 20
    private const val STEP = 4
    private const val MIN_CUT = 0.02
    private const val MIN_KEEP = 0.5

    fun area(
        width: Int,
        height: Int,
        pixel: (Int, Int) -> Int,
    ): Area? {
        if (width < 2 || height < 2) return null
        val corner = pixel(0, 0)
        val margin: (Int) -> Boolean =
            when {
                isLight(corner) -> ::isLight
                isDark(corner) -> ::isDark
                else -> return null
            }
        val rows = blankEdges(height) { y -> (0 until width step STEP).all { margin(pixel(it, y)) } }
        val cols = blankEdges(width) { x -> (rows.first until rows.last step STEP).all { margin(pixel(x, it)) } }
        val area = Area(cols.first, rows.first, cols.last, rows.last)
        return area.takeIf { worthCutting(area, width, height) }
    }

    /** First and past-the-last line that isn't margin, from both ends. */
    private fun blankEdges(
        size: Int,
        blank: (Int) -> Boolean,
    ): IntRange {
        var start = 0
        while (start < size - 1 && blank(start)) start++
        var end = size
        while (end - 1 > start && blank(end - 1)) end--
        return start..end
    }

    private fun worthCutting(
        area: Area,
        width: Int,
        height: Int,
    ): Boolean {
        val cutSide = maxOf(area.left, width - area.right).toDouble() / width
        val cutTop = maxOf(area.top, height - area.bottom).toDouble() / height
        val keeps = area.width >= width * MIN_KEEP && area.height >= height * MIN_KEEP
        return (cutSide >= MIN_CUT || cutTop >= MIN_CUT) && keeps
    }

    private const val RED = 16
    private const val GREEN = 8
    private const val BYTE = 0xFF

    private fun channels(argb: Int) = listOf((argb shr RED) and BYTE, (argb shr GREEN) and BYTE, argb and BYTE)

    private fun isLight(argb: Int) = channels(argb).all { it >= LIGHT }

    private fun isDark(argb: Int) = channels(argb).all { it <= DARK }
}
