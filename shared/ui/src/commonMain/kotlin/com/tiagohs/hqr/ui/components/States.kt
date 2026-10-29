package com.tiagohs.hqr.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.ui.theme.BalloonShape
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Loading (RF-86): the burst indicator; the onomatopoeia joins only after 1.5 s (design.md §5). */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1_500)
        slow = true
    }
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BurstIndicator()
        if (slow) {
            Text(
                "VRUUUM!",
                fontFamily = bangers(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The comic "burst" shape spinning: the loading indicator of the design (M3 Expressive shape, stable API). */
@Composable
fun BurstIndicator(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val angle by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)))
    Canvas(modifier.size(48.dp).semantics { contentDescription = "…" }) {
        val c = center
        val outer = size.minDimension / 2
        val inner = outer * 0.72f
        val path = Path()
        for (i in 0 until 24) {
            val r = if (i % 2 == 0) outer else inner
            val a = (i * 15f + angle) * (PI.toFloat() / 180f)
            val x = c.x + r * cos(a)
            val y = c.y + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

/** Empty state: title in a balloon, one sentence and one action (ux-foundations §8). */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onomatopoeia: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
    secondary: String? = null,
    onSecondary: () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onomatopoeia != null) {
            Text(
                onomatopoeia,
                fontFamily = bangers(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, BalloonShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { },
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (action != null) Button(onClick = onAction) { Text(action) }
            if (secondary != null) OutlinedButton(onClick = onSecondary) { Text(secondary) }
        }
    }
}

/**
 * An error inside content (design.md §5): surface, 4 px error stripe at the start, icon, sentence and an action that
 * says the fix.
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
    icon: ImageVector = HqrIcons.Error,
) {
    val stripe = MaterialTheme.colorScheme.error
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .drawBehind { drawRect(stripe, size = size.copy(width = 4.dp.toPx())) }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = stripe, modifier = Modifier.size(20.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

/** A full-screen error with the fix (RF-86): used when nothing can be shown. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
) {
    EmptyState(title, message, modifier, null, action, onAction, secondary, onSecondary)
}

/** The discreet offline banner (RF-87). */
@Composable
fun OfflineBanner(text: String) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(HqrIcons.Offline, contentDescription = null, modifier = Modifier.size(18.dp))
        Box(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
