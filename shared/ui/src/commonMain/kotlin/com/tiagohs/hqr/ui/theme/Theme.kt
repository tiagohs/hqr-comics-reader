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

/** Material color scheme from a seed: the violet base or the cover color (RF-98). */
fun hqrColorScheme(
    seed: Int,
    dark: Boolean,
): ColorScheme {
    val r = CoverColor.roles(seed, dark)
    val (error, errorContainer, onErrorContainer) = if (dark) errorDark else errorLight
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
        background = Color(r.background),
        onBackground = Color(r.onSurface),
        surface = Color(r.background),
        onSurface = Color(r.onSurface),
        surfaceVariant = Color(r.surface3),
        onSurfaceVariant = Color(r.onSurfaceVariant),
        surfaceContainerLowest = Color(r.background),
        surfaceContainerLow = Color(r.surface1),
        surfaceContainer = Color(r.surface1),
        surfaceContainerHigh = Color(r.surface2),
        surfaceContainerHighest = Color(r.surface3),
        surfaceBright = Color(r.surface3),
        surfaceDim = Color(r.background),
        outline = Color(r.outline),
        outlineVariant = Color(r.outlineVariant),
        inverseSurface = Color(r.onSurface),
        inverseOnSurface = Color(r.surface1),
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
