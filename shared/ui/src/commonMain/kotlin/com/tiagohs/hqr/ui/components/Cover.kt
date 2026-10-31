package com.tiagohs.hqr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.ui.theme.HqrShapes
import com.tiagohs.hqr.ui.theme.bangers

/** Halftone dots (design.md §4): 2 px dots every 6 px. */
fun Modifier.halftone(
    color: Color,
    step: Float = 6f,
    radius: Float = 1.2f,
): Modifier =
    drawWithContent {
        drawContent()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color, radius * density, Offset(x * density, y * density))
                x += step
            }
            y += step
        }
    }

/** A cover with the panel cut (2:3), a placeholder in the same shape while loading, and badge slots. */
@Composable
fun Cover(
    manga: SourceManga,
    modifier: Modifier = Modifier,
    shape: Shape = HqrShapes.cover,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // ADR-026: every cover is a comic panel with an ink frame.
            .border(2.dp, MaterialTheme.colorScheme.onSurface, shape),
    ) {
        val url = manga.coverUrl
        if (url != null) {
            SubcomposeAsyncImage(
                model = ImageRef(manga.sourceId, url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { CoverFallback(manga.title) },
            )
        } else {
            CoverFallback(manga.title)
        }
        overlay()
    }
}

@Composable
private fun CoverFallback(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .halftone(MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, maxLines = 3, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Unread count in a comic burst (ADR-026), top end of the cover. */
@Composable
fun BoxScope.UnreadBalloon(count: Int) {
    if (count <= 0) return
    Burst(if (count > 99) "99+" else count.toString(), Modifier.align(Alignment.TopEnd).padding(2.dp), size = 34.dp)
}

/** "Na biblioteca" badge, one style everywhere (design.md §5). */
@Composable
fun BoxScope.InLibraryBadge(label: String) {
    Text(
        text = label,
        fontFamily = bangers(),
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
