package com.tiagohs.hqr.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import com.tiagohs.hqr.domain.model.SourceManga
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/**
 * RF-118, design.md §4: the comic motion. Speed lines come in from the edges, the vertical title writes itself, stamps
 * land like rubber stamps, lists cascade once and the cover flies into the work tilting 2.5°. Every movement stays
 * between 150 and 480 ms; with "reduzir movimento" everything becomes a 150 ms fade.
 */
object ComicMotion {
    const val FADE_MS = 150
    const val PANEL_MS = 420
    const val STAMP_MS = 280
    const val CASCADE_MS = 240
    const val SKETCH_MS = 460
    const val CASCADE_STEP_MS = 30
    const val CASCADE_ROWS = 8

    /** Rows past the first [CASCADE_ROWS] arrive with the last one: the list never waits on a long cascade. */
    fun cascadeDelay(
        index: Int,
        reduce: Boolean,
    ): Int = if (reduce) 0 else index.coerceIn(0, CASCADE_ROWS) * CASCADE_STEP_MS

    fun duration(
        ms: Int,
        reduce: Boolean,
    ): Int = if (reduce) FADE_MS else ms

    /** The whole vertical title takes at most 480 ms, however many letters. */
    fun writeMs(count: Int): Int = (LETTER_FADE_MS + count.coerceAtLeast(1) * LETTER_STEP_MS).coerceAtMost(MAX_MS)

    /** How visible the [index]-th letter is when the title is [progress] (0 to 1) written. */
    fun letterAlpha(
        index: Int,
        count: Int,
        progress: Float,
        reduce: Boolean,
    ): Float {
        if (reduce) return 1f
        val total = writeMs(count).toFloat()
        val start = index * LETTER_STEP_MS * (total / (LETTER_FADE_MS + count * LETTER_STEP_MS))
        return ((progress * total - start) / LETTER_FADE_MS).coerceIn(0f, 1f)
    }

    private const val LETTER_STEP_MS = 45
    private const val LETTER_FADE_MS = 140
    private const val MAX_MS = 480
}

/** RF-118: true when the system asks for less motion; the screens then only fade. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** The system setting: "remover animações" on Android, "prefers-reduced-motion" in the browser. */
@Composable
expect fun rememberSystemReduceMotion(): Boolean

/** The shared transition around the screens and the scope of the screen being shown (the cover's flight). */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }
val LocalScreenAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** RF-118: the same cover in a list and on the work screen is one element that flies between them. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCover(manga: SourceManga): Modifier {
    val shared = LocalSharedTransition.current
    val screen = LocalScreenAnimation.current
    if (shared == null || screen == null || LocalReduceMotion.current) return this
    return with(shared) {
        this@sharedCover.sharedElement(
            rememberSharedContentState("cover/${manga.sourceId.value}/${manga.key}"),
            screen,
            boundsTransform = { _, _ -> tween(ComicMotion.PANEL_MS, easing = FastOutSlowInEasing) },
        )
    }
}

/** 0 to 1 once, when the element first shows; at once with "reduzir movimento". */
@Composable
fun rememberEntrance(
    durationMs: Int = ComicMotion.PANEL_MS,
    delayMs: Int = 0,
): Animatable<Float, *> {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(delayMs.toLong())
            progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
        }
    }
    return progress
}

/** "NOVO!" and other stamps land like a rubber stamp: big and tilted, then pressed into place. */
@Composable
fun Modifier.stampIn(): Modifier {
    val reduce = LocalReduceMotion.current
    val press = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (press.value < 1f) press.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = STAMP_STIFFNESS))
    }
    return graphicsLayer {
        val p = press.value
        alpha = p.coerceIn(0f, 1f)
        val scale = 1f + (1f - p) * STAMP_GROWTH
        scaleX = scale
        scaleY = scale
        rotationZ = (1f - p) * STAMP_TURN
    }
}

/**
 * Lists cascade only while the screen opens ([since]); rows that scroll in later just show, so scrolling never
 * waits on the motion.
 */
@Composable
fun Modifier.cascadeIn(
    index: Int,
    since: TimeSource.Monotonic.ValueTimeMark,
): Modifier {
    val reduce = LocalReduceMotion.current
    val opening = since.elapsedNow().inWholeMilliseconds < OPENING_MS
    val progress = remember { Animatable(if (reduce || !opening) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(ComicMotion.cascadeDelay(index, reduce).toLong())
            progress.animateTo(1f, tween(ComicMotion.CASCADE_MS, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * CASCADE_RISE * density
    }
}

/** When a screen opened, for [cascadeIn]. */
@Composable
fun rememberOpening(): TimeSource.Monotonic.ValueTimeMark = remember { TimeSource.Monotonic.markNow() }

/**
 * A frame drawn by hand: two loose strokes around the content whose corners run past each other, traced in as
 * [progress] goes from 0 to 1 (the first-run cards, RF-118).
 */
fun Modifier.sketchFrame(
    color: androidx.compose.ui.graphics.Color,
    width: androidx.compose.ui.unit.Dp,
    progress: () -> Float,
): Modifier =
    drawWithContent {
        drawContent()
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0f) return@drawWithContent
        val over = SKETCH_OVERRUN * density
        val stroke =
            androidx.compose.ui.graphics.drawscope
                .Stroke(width.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        listOf(0f to 0f, SKETCH_WOBBLE * density to -SKETCH_WOBBLE * density).forEachIndexed { pass, (dx, dy) ->
            val path =
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(-over + dx, dy)
                    lineTo(size.width + over * 0.4f + dx, dy - pass)
                    moveTo(size.width + dx, -over + dy)
                    lineTo(size.width - pass + dx, size.height + over * 0.5f + dy)
                    moveTo(size.width + over + dx, size.height + dy)
                    lineTo(-over * 0.3f + dx, size.height + pass + dy)
                    moveTo(dx, size.height + over + dy)
                    lineTo(dx + pass, -over * 0.6f + dy)
                }
            val measure =
                androidx.compose.ui.graphics
                    .PathMeasure()
                    .apply { setPath(path, false) }
            val traced =
                androidx.compose.ui.graphics
                    .Path()
            // The second stroke follows the first, a little behind.
            val share = ((p - pass * SKETCH_LAG) / (1f - SKETCH_LAG)).coerceIn(0f, 1f)
            measure.getSegment(0f, measure.length * share, traced, true)
            drawPath(traced, color, alpha = if (pass == 0) 1f else SKETCH_SECOND_ALPHA, style = stroke)
        }
    }

private const val SKETCH_OVERRUN = 6f
private const val SKETCH_WOBBLE = 1.5f
private const val SKETCH_LAG = 0.25f
private const val SKETCH_SECOND_ALPHA = 0.55f
private const val OPENING_MS = 700
private const val CASCADE_RISE = 14f
private const val STAMP_GROWTH = 0.9f
private const val STAMP_TURN = -10f
private const val STAMP_STIFFNESS = 500f
