package com.tiagohs.hqr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The comic panel cut of covers (design.md §4, `shape.cover-cut`): corners of 10 and the bottom-right corner
 * chamfered by 14. Mirrored in right-to-left layouts.
 */
class CoverCutShape(
    private val radius: Dp = 10.dp,
    private val cut: Dp = 14.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { radius.toPx() }
        val c = with(density) { cut.toPx() }
        val rounded = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
        val corner =
            Path().apply {
                val rtl = layoutDirection == LayoutDirection.Rtl
                val x = if (rtl) 0f else size.width
                val dx = if (rtl) c else -c
                moveTo(x, size.height - c)
                lineTo(x, size.height)
                lineTo(x + dx, size.height)
                close()
            }
        return Outline.Generic(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, rounded, corner))
    }
}

/** Speech-balloon badge: rounded with the bottom-left corner almost square (the tail), design.md §4. */
val BalloonShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 2.dp)

object HqrShapes {
    val cover = CoverCutShape()
    val coverSmall = CoverCutShape(radius = 8.dp, cut = 10.dp)
    val material =
        Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp),
        )
}
