package com.tiagohs.hqr.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.dela_gothic_one_jp
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.bangers
import org.jetbrains.compose.resources.Font
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The comic language of ADR-026: lettering, badges, halftone and speed lines. Controls stay Material 3.

/** Ben-Day / halftone dots behind the content (design.md §4). */
fun Modifier.halftoneBehind(
    color: Color,
    step: Dp = 6.dp,
    radius: Dp = 1.1.dp,
    until: Float = 1f,
    progress: () -> Float = { 1f },
): Modifier =
    drawBehind {
        val s = step.toPx()
        val r = radius.toPx()
        var y = s / 2
        var row = 0
        // RF-118: [progress] lets the dots come down the panel like ink being printed.
        while (y < size.height * until * progress().coerceIn(0f, 1f)) {
            var x = if (row % 2 == 0) s / 2 else s
            while (x < size.width) {
                drawCircle(color, r, Offset(x, y))
                x += s
            }
            y += s / 2 * 1.73f
            row++
        }
    }

/**
 * Manga speed lines (design.md §4): hairlines that stay at the edges of the panel, uneven like ink, leaving the middle
 * clear for the cover. [progress] (0 to 1) draws them in from the edges toward the center (RF-118).
 */
fun Modifier.speedLines(
    color: Color,
    stepDegrees: Float = 6f,
    progress: () -> Float = { 1f },
): Modifier =
    drawBehind {
        val c = center
        val reach = kotlin.math.hypot(size.width / 2, size.height / 2)
        val p = progress().coerceIn(0f, 1f)
        var a = 0f
        var i = 0
        while (a < 360f) {
            val rad = (a + JITTER[i % JITTER.size]) * (PI.toFloat() / 180f)
            // Uneven lengths, as drawn by hand; none closer to the center than 62 % of the way.
            val rest = reach * (SPEED_INNER + SPEED_SPREAD * LENGTH[i % LENGTH.size])
            val inner = reach - (reach - rest) * p
            drawLine(
                color,
                Offset(c.x + inner * cos(rad), c.y + inner * sin(rad)),
                Offset(c.x + reach * cos(rad), c.y + reach * sin(rad)),
                strokeWidth = (if (i % 3 == 0) 1f else 0.6f).dp.toPx(),
            )
            a += stepDegrees
            i++
        }
    }

private const val SPEED_INNER = 0.62f
private const val SPEED_SPREAD = 0.22f
private val JITTER = floatArrayOf(0f, 1.6f, -1.1f, 2.3f, -0.4f, 0.9f, -2f)
private val LENGTH = floatArrayOf(0f, 0.55f, 0.2f, 0.9f, 0.35f, 0.7f, 0.1f, 0.45f)

/** Comic title: Bangers with the offset shadow in the primary color. */
@Composable
fun ComicTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    maxLines: Int = 2,
) {
    Text(
        text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style =
            style.copy(
                fontFamily = bangers(),
                letterSpacing = 1.sp,
                shadow = Shadow(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), Offset(3f, 3f), 0f),
            ),
    )
}

/** "NOVO!" and other stamps: Bangers on the primary color, tilted like a rubber stamp. */
@Composable
fun Stamp(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Text(
        text,
        fontFamily = bangers(),
        fontSize = 15.sp,
        letterSpacing = 1.sp,
        color = content,
        // RF-118: it lands like a rubber stamp.
        modifier =
            modifier
                .stampIn()
                .rotate(-4f)
                .background(container)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** The comic explosion: 16 points, for counts (+1) and "OPS!". */
val BurstShape: Shape =
    GenericShape { size, _ ->
        val c = Offset(size.width / 2, size.height / 2)
        val points = 16
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) size.minDimension / 2 else size.minDimension * 0.36f
            val a = (i * 180f / points - 90f) * (PI.toFloat() / 180f)
            val p = Offset(c.x + r * cos(a), c.y + r * sin(a))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

@Composable
fun Burst(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(modifier.size(size).clip(BurstShape).background(container), contentAlignment = Alignment.Center) {
        Text(text, fontFamily = bangers(), fontSize = (size.value * 0.32f).sp, color = content)
    }
}

/** Work facts as Material assist chips: author when known, status with its dot, and the source (SCR-OBR-001). */
@Composable
fun MetaTags(
    author: String?,
    status: String?,
    statusColor: Color,
    source: String?,
    modifier: Modifier = Modifier,
    extra: List<String> = emptyList(),
    onSource: (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        val colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        author?.let {
            AssistChip(
                onClick = {},
                label = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(HqrIcons.Person, null, Modifier.size(AssistChipDefaults.IconSize)) },
                colors = colors,
            )
        }
        status?.let {
            AssistChip(
                onClick = {},
                label = { Text(it) },
                leadingIcon = { Box(Modifier.size(8.dp).background(statusColor, CircleShape)) },
                colors = colors,
            )
        }
        source?.let {
            AssistChip(
                onClick = { onSource?.invoke() },
                enabled = true,
                label = { Text(it) },
                leadingIcon = { Icon(HqrIcons.Globe, null, Modifier.size(AssistChipDefaults.IconSize)) },
                colors = colors,
            )
        }
        extra.forEach { AssistChip(onClick = {}, label = { Text(it) }, colors = colors) }
    }
}

/** Soft loading block: rounded, no border, a light sweeping slowly across (ux-foundations §8). */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shine = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f)
    val x by rememberInfiniteTransition().animateFloat(-1f, 2f, infiniteRepeatable(tween(1_600, easing = LinearEasing)))
    Box(
        modifier.clip(shape).background(base).drawBehind {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, shine, Color.Transparent),
                    start = Offset(w * (x - 0.5f), 0f),
                    end = Offset(w * (x + 0.5f), size.height),
                ),
            )
        },
    )
}

/** Three dots pulsing in the primary color, next to "Buscando no MangaDex…". */
@Composable
fun DotsLoader(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val t by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart))
    Row(
        modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val phase = ((t - i * 0.15f) % 1f + 1f) % 1f
            val on =
                if (phase < 0.4f) {
                    phase / 0.4f
                } else if (phase < 0.8f) {
                    1f - (phase - 0.4f) / 0.4f
                } else {
                    0f
                }
            Box(Modifier.size(6.dp).alpha(0.25f + 0.75f * on).background(color, CircleShape))
        }
    }
}

/** The work's original title written top to bottom, with an ink outline (manga hero only). */
@Composable
fun VerticalTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 32,
) {
    val family =
        androidx.compose.ui.text.font
            .FontFamily(Font(Res.font.dela_gothic_one_jp))
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.background
    val letters = text.take(MAX_VERTICAL)
    // RF-118: written from top to bottom, one letter after the other.
    val reduce = LocalReduceMotion.current
    val written = rememberEntrance(ComicMotion.writeMs(letters.length), delayMs = ComicMotion.PANEL_MS / 2)
    Column(modifier.clearAndSetSemantics { }, horizontalAlignment = Alignment.CenterHorizontally) {
        letters.forEachIndexed { index, ch ->
            Box(
                Modifier.graphicsLayer {
                    val shown = ComicMotion.letterAlpha(index, letters.length, written.value, reduce)
                    alpha = shown
                    translationY = (1f - shown) * -6f * density
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ch.toString(),
                    fontFamily = family,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize + 2).sp,
                    color = ink,
                    style = TextStyle(drawStyle = Stroke(width = 5f)),
                )
                Text(ch.toString(), fontFamily = family, fontSize = fontSize.sp, lineHeight = (fontSize + 2).sp, color = paper)
            }
        }
    }
}

/** Japanese text that the subset font can draw: kana or kanji, no Latin letters. */
fun String.isJapanese(): Boolean =
    isNotBlank() && length <= MAX_VERTICAL && all { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF || it.code in 0x3000..0x303F }

private const val MAX_VERTICAL = 7
