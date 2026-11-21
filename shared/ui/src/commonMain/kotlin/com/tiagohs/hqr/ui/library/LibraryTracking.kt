package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.presentation.library.LibraryArea
import com.tiagohs.hqr.presentation.library.ProgressSummary
import com.tiagohs.hqr.presentation.library.ReadingState
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.library_area_comics
import com.tiagohs.hqr.ui.resources.library_area_manga
import com.tiagohs.hqr.ui.resources.library_progress_chapters
import com.tiagohs.hqr.ui.resources.library_progress_read
import com.tiagohs.hqr.ui.resources.library_progress_ring
import com.tiagohs.hqr.ui.resources.library_progress_title
import com.tiagohs.hqr.ui.resources.library_progress_type
import com.tiagohs.hqr.ui.resources.library_progress_works
import com.tiagohs.hqr.ui.resources.library_state_all
import com.tiagohs.hqr.ui.resources.library_state_finished
import com.tiagohs.hqr.ui.resources.library_state_group
import com.tiagohs.hqr.ui.resources.library_state_new
import com.tiagohs.hqr.ui.resources.library_state_pending
import com.tiagohs.hqr.ui.resources.library_state_reading
import com.tiagohs.hqr.ui.resources.library_state_up_to_date
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** RF-132: the label of each situation, as the shelf and its chips say it. */
fun ReadingState.label(): StringResource =
    when (this) {
        ReadingState.PENDING -> Res.string.library_state_pending
        ReadingState.READING -> Res.string.library_state_reading
        ReadingState.NEW_CHAPTERS -> Res.string.library_state_new
        ReadingState.UP_TO_DATE -> Res.string.library_state_up_to_date
        ReadingState.FINISHED -> Res.string.library_state_finished
    }

/** RF-132: tokens.json comic.state-*; "Lidas" takes the scheme's primary, "Pendentes" a neutral ink. */
@Composable
fun ReadingState.color(): Color =
    when (this) {
        ReadingState.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = PENDING_ALPHA)
        ReadingState.READING -> STATE_READING
        ReadingState.NEW_CHAPTERS -> STATE_NEW
        ReadingState.UP_TO_DATE -> STATE_UP_TO_DATE
        ReadingState.FINISHED -> MaterialTheme.colorScheme.primary
    }

/** The order of the bar and the chips: what asks for attention first. */
internal val SHELF_ORDER =
    listOf(ReadingState.READING, ReadingState.NEW_CHAPTERS, ReadingState.UP_TO_DATE, ReadingState.FINISHED, ReadingState.PENDING)

/**
 * RF-132: the large ring with the % read, the two numbers as cards below, the bar by situation and its chips, which
 * open the shelf in that situation; "Tudo" adds HQs and Mangás apart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressCard(
    progress: ProgressSummary,
    perArea: Map<LibraryArea, ProgressSummary>?,
    selected: ReadingState?,
    onSelect: (ReadingState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.library_progress_title)
    Surface(
        modifier.fillMaxWidth().semantics { contentDescription = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp)) {
            ProgressRing(progress.percent, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kpi("${progress.worksRead}", "/${progress.works}", stringResource(Res.string.library_progress_works), Modifier.weight(1f))
                Kpi(progress.chaptersRead.grouped(), "", stringResource(Res.string.library_progress_chapters), Modifier.weight(1f))
            }
            val parts = SHELF_ORDER.mapNotNull { s -> progress.byState[s]?.let { s to it } }
            if (parts.isNotEmpty()) {
                val names = parts.map { (s, n) -> "$n ${stringResource(s.label())}" }.joinToString(", ")
                Row(
                    Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .semantics { contentDescription = names },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) { parts.forEach { (s, n) -> Box(Modifier.weight(n.toFloat()).fillMaxSize().background(s.color())) } }
                val group = stringResource(Res.string.library_state_group)
                FlowRow(
                    Modifier.padding(top = 12.dp).semantics { contentDescription = group },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    parts.forEach { (s, n) ->
                        FilterChip(
                            selected = selected == s,
                            onClick = { onSelect(if (selected == s) null else s) },
                            label = { Text("${stringResource(s.label())} $n") },
                            leadingIcon = {
                                if (selected == s) {
                                    Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize))
                                } else {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(s.color()))
                                }
                            },
                        )
                    }
                }
            }
            perArea?.let { areas ->
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeRow(stringResource(Res.string.library_area_comics), areas[LibraryArea.COMICS])
                    TypeRow(stringResource(Res.string.library_area_manga), areas[LibraryArea.MANGA])
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.library_progress_ring, "$percent%")
    Box(modifier.size(RING).clearAndSetSemantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { percent / PERCENT },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA),
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                stringResource(Res.string.library_progress_read),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Kpi(
    value: String,
    suffix: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                if (suffix.isNotEmpty()) {
                    Text(
                        suffix,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypeRow(
    name: String,
    summary: ProgressSummary?,
) {
    val s = summary ?: return
    if (s.works == 0) return
    val label = stringResource(Res.string.library_progress_type, name, s.worksRead, s.works)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(64.dp))
        LinearProgressIndicator(progress = { s.percent / PERCENT }, modifier = Modifier.weight(1f), strokeCap = StrokeCap.Round)
        Text("${s.worksRead}/${s.works}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** RF-132: "Estante" by situation, one chip per situation that has works, plus "Todas". */
@Composable
fun ShelfStateChips(
    progress: ProgressSummary,
    selected: ReadingState?,
    onSelect: (ReadingState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts = SHELF_ORDER.mapNotNull { s -> progress.byState[s]?.let { s to it } }
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("${stringResource(Res.string.library_state_all)} ${progress.works}") },
                leadingIcon = if (selected == null) ({ Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
            )
        }
        items(parts, key = { it.first.name }) { (s, n) ->
            FilterChip(
                selected = selected == s,
                onClick = { onSelect(s) },
                label = { Text("${stringResource(s.label())} $n") },
                leadingIcon = if (selected == s) ({ Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
            )
        }
    }
}

private fun Int.grouped(): String = toString()

/** A header in a grid with its own side padding reaches the screen edges (the grid pads its cells). */
fun Modifier.fullBleed(side: androidx.compose.ui.unit.Dp): Modifier =
    layout { measurable, constraints ->
        val px = side.roundToPx()
        val placeable =
            measurable.measure(constraints.copy(minWidth = constraints.minWidth + 2 * px, maxWidth = constraints.maxWidth + 2 * px))
        layout(placeable.width - 2 * px, placeable.height) { placeable.place(-px, 0) }
    }

internal const val PERCENT = 100f
internal const val PERCENT_INT = 100
internal const val NEW_LIMIT = 3
internal const val PENDING_ALPHA = 0.3f
internal const val TRACK_ALPHA = 0.14f
internal val RING = 176.dp
internal val CAROUSEL_ITEM = 262.dp
internal val CAROUSEL_HEIGHT = 220.dp

// tokens.json color.comic.
internal val STATE_READING = Color(0xFFF2C14E)
internal val STATE_UP_TO_DATE = Color(0xFF7FD1AE)
internal val STATE_UP_TO_DATE_INK = Color(0xFF0B2A1E)
internal val STATE_NEW = Color(0xFFFFF1A8)
internal val PAPER = Color(0xFFF6F1E7)
internal val INK = Color(0xFF16161A)
internal val CORNER_DARK = Color(0xCC16161A)
internal val SCRIM_STOPS = listOf(Color.Transparent, Color.Transparent, Color(0xD1000000))
