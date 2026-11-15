package com.tiagohs.hqr.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tiagohs.hqr.presentation.color.CoverColor

/** Error colors are fixed (design.md §2); only the content roles follow the cover. */
private val errorLight = Triple(Color(0xFFBA1A1A), Color(0xFFFFDAD6), Color(0xFF410002))
private val errorDark = Triple(Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFDAD6))

/**
 * Paper and ink (ADR-026): the neutral roles are fixed, warm paper in light and ink in dark, so every screen reads
 * like a printed page; only the accent roles follow the seed.
 */
private data class Paper(
    val background: Color,
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

private val paperLight =
    Paper(
        background = Color(0xFFF6F1E7),
        lowest = Color(0xFFFFFDF7),
        low = Color(0xFFFBF7EE),
        container = Color(0xFFF1ECE1),
        high = Color(0xFFECE6DA),
        highest = Color(0xFFE5DFD2),
        onSurface = Color(0xFF16161A),
        onSurfaceVariant = Color(0xFF55545A),
        outline = Color(0xFF77757C),
        outlineVariant = Color(0xFFCFC8BB),
    )

private val paperDark =
    Paper(
        background = Color(0xFF16161A),
        lowest = Color(0xFF0F0F12),
        low = Color(0xFF1C1C21),
        container = Color(0xFF222127),
        high = Color(0xFF2A292F),
        highest = Color(0xFF35343A),
        onSurface = Color(0xFFF6F1E7),
        onSurfaceVariant = Color(0xFFC4C0C8),
        outline = Color(0xFF8F8C94),
        outlineVariant = Color(0xFF3E3D43),
    )

/** Material color scheme from a seed: the violet base or the cover color (RF-98), over paper and ink. */
fun hqrColorScheme(
    seed: Int,
    dark: Boolean,
): ColorScheme {
    val r = CoverColor.roles(seed, dark)
    val (error, errorContainer, onErrorContainer) = if (dark) errorDark else errorLight
    val p = if (dark) paperDark else paperLight
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(r.primary),
        onPrimary = Color(r.onPrimary),
        primaryContainer = Color(r.primaryContainer),
        onPrimaryContainer = Color(r.onPrimaryContainer),
        secondary = Color(r.primary),
        onSecondary = Color(r.onPrimary),
        secondaryContainer = Color(r.primaryContainer),
        onSecondaryContainer = Color(r.onPrimaryContainer),
        tertiary = Color(r.primary),
        onTertiary = Color(r.onPrimary),
        tertiaryContainer = Color(r.primaryContainer),
        onTertiaryContainer = Color(r.onPrimaryContainer),
        background = p.background,
        onBackground = p.onSurface,
        surface = p.background,
        onSurface = p.onSurface,
        surfaceVariant = p.highest,
        onSurfaceVariant = p.onSurfaceVariant,
        surfaceContainerLowest = p.lowest,
        surfaceContainerLow = p.low,
        surfaceContainer = p.container,
        surfaceContainerHigh = p.high,
        surfaceContainerHighest = p.highest,
        surfaceBright = p.highest,
        surfaceDim = p.background,
        outline = p.outline,
        outlineVariant = p.outlineVariant,
        inverseSurface = p.onSurface,
        inverseOnSurface = p.background,
        inversePrimary = Color(CoverColor.roles(seed, !dark).primary),
        error = error,
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        scrim = Color.Black,
    )
}

/** RF-76: the "preto puro" setting, given once by the app for every theme below it. */
val LocalPureBlack = staticCompositionLocalOf { false }

/** RF-76: the system's colors instead of the violet base, where the platform has them. */
val LocalDynamicColor = staticCompositionLocalOf { false }

/**
 * The app theme. [seed] is the violet base everywhere except the work screen, the reader and the work detail pane,
 * which pass the cover color (RF-98). The change animates in 300 ms; with reduced motion it switches at once.
 */
@Composable
fun HqrTheme(
    dark: Boolean,
    seed: Int = CoverColor.VIOLET,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    // RF-76: the system's colors replace the violet base only; a cover color (RF-98) still wins on its screens.
    val system = if (seed == CoverColor.VIOLET && LocalDynamicColor.current) dynamicColorScheme(dark) else null
    val base = system ?: hqrColorScheme(seed, dark)
    // RF-76: "preto puro" turns the dark backgrounds to #000000.
    val target = if (dark && LocalPureBlack.current) base.copy(background = Color.Black, surface = Color.Black) else base
    val scheme = if (animate) target.animated() else target
    MaterialTheme(colorScheme = scheme, typography = hqrTypography(), shapes = HqrShapes.material, content = content)
}

@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun Color.anim(): Color {
        val value by animateColorAsState(this, tween(300))
        return value
    }
    return copy(
        primary = primary.anim(),
        onPrimary = onPrimary.anim(),
        primaryContainer = primaryContainer.anim(),
        onPrimaryContainer = onPrimaryContainer.anim(),
        background = background.anim(),
        surface = surface.anim(),
        surfaceContainerLow = surfaceContainerLow.anim(),
        surfaceContainer = surfaceContainer.anim(),
        surfaceContainerHigh = surfaceContainerHigh.anim(),
        surfaceContainerHighest = surfaceContainerHighest.anim(),
        onSurface = onSurface.anim(),
        onSurfaceVariant = onSurfaceVariant.anim(),
        outline = outline.anim(),
        outlineVariant = outlineVariant.anim(),
    )
}
