package com.tiagohs.hqr.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.ui.components.ComicTitle
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.MetaTags
import com.tiagohs.hqr.ui.components.Skeleton
import com.tiagohs.hqr.ui.components.VerticalTitle
import com.tiagohs.hqr.ui.components.halftoneBehind
import com.tiagohs.hqr.ui.components.isJapanese
import com.tiagohs.hqr.ui.components.label
import com.tiagohs.hqr.ui.components.speedLines
import com.tiagohs.hqr.ui.components.unavailableSourceName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_less_text
import com.tiagohs.hqr.ui.resources.action_more_text
import com.tiagohs.hqr.ui.resources.source_unavailable
import com.tiagohs.hqr.ui.theme.bangers
import org.jetbrains.compose.resources.stringResource

// The top of the work screen (SCR-OBR-001) in the comic language of ADR-026: panel, title, tags, synopsis.

/** A round icon button over the hero, readable on any cover color. */
@Composable
internal fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    androidx.compose.material3.OutlinedIconButton(
        onClick = onClick,
        colors =
            androidx.compose.material3.IconButtonDefaults.outlinedIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        modifier = Modifier.padding(horizontal = 4.dp),
    ) { Icon(icon, label) }
}

/** RF-117: a soft row in the place of a chapter still on its way. */
@Composable
internal fun ChapterSkeleton(index: Int) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Skeleton(Modifier.size(width = 40.dp, height = 28.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Skeleton(Modifier.fillMaxWidth(0.7f - index * 0.08f).height(14.dp))
            Skeleton(Modifier.fillMaxWidth(0.4f).height(10.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
}

internal const val SKELETON_ROWS = 4

/**
 * The work's panel (ADR-026): the tilted cover over speed lines and the vertical original title for manga, or over
 * Ben-Day dots for comics, with the genres as tags below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Hero(
    manga: Manga,
    topPadding: androidx.compose.ui.unit.Dp,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val vertical =
        manga.source.alternativeTitles
            .firstOrNull { it.trim().isJapanese() }
            ?.trim()
    val art =
        if (vertical != null) {
            Modifier.speedLines(ink.copy(alpha = 0.3f))
        } else {
            Modifier.halftoneBehind(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), step = 9.dp, radius = 2.4.dp)
        }
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer
                    .copy(alpha = HERO_TINT)
                    .compositeOver(MaterialTheme.colorScheme.background),
            ).then(art)
            .padding(top = topPadding, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(140.dp)
                    .rotate(-2.5f)
                    .drawBehind {
                        drawRect(
                            ink,
                            topLeft =
                                androidx.compose.ui.geometry
                                    .Offset(6.dp.toPx(), 6.dp.toPx()),
                            size = size,
                        )
                    },
            ) { Cover(manga.source) }
            if (vertical != null) VerticalTitle(vertical, Modifier.align(Alignment.TopEnd).padding(end = 16.dp))
        }
        val genres = manga.source.genres.take(MAX_HERO_GENRES)
        if (genres.isNotEmpty()) {
            FlowRow(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { genres.forEach { GenreTag(it) } }
        }
    }
    HorizontalDivider(thickness = 3.dp, color = ink)
}

@Composable
internal fun GenreTag(text: String) {
    Text(
        text,
        fontFamily = bangers(),
        fontSize = 15.sp,
        letterSpacing = 1.sp,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, MaterialTheme.colorScheme.onSurface)
                .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Title, original title, the facts as tags (author, status, source) and the whole synopsis (RF-29). */
@Composable
internal fun Header(
    manga: Manga,
    sourceLabel: String,
) {
    val source = manga.source
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ComicTitle(source.title, style = MaterialTheme.typography.headlineLarge, maxLines = 4)
        if (source.alternativeTitles.isNotEmpty()) {
            Text(
                source.alternativeTitles.take(2).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val people = (source.authors + source.artists).distinct().take(2)
        MetaTags(
            author = people.takeIf { it.isNotEmpty() }?.joinToString(" · "),
            status = source.status.label()?.let { stringResource(it) },
            statusColor = statusColor(source.status),
            source = sourceLabel,
            extra = listOfNotNull(source.language?.uppercase()),
        )
        unavailableSourceName(manga.sourceId)?.let { name ->
            Text(
                stringResource(Res.string.source_unavailable, name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Synopsis(source.description, source.alternativeTitles)
    }
}

@Composable
internal fun statusColor(status: com.tiagohs.hqr.domain.model.PublicationStatus): Color =
    when (status) {
        com.tiagohs.hqr.domain.model.PublicationStatus.ONGOING -> Color(0xFF2E7D32)
        com.tiagohs.hqr.domain.model.PublicationStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        com.tiagohs.hqr.domain.model.PublicationStatus.HIATUS -> Color(0xFFB26A00)
        com.tiagohs.hqr.domain.model.PublicationStatus.CANCELLED -> MaterialTheme.colorScheme.error
        com.tiagohs.hqr.domain.model.PublicationStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

internal const val MAX_HERO_GENRES = 4
internal const val HERO_TINT = 0.55f

/** The synopsis shows whole; only a very long one folds, with "Mais" (RF-29). */
@Composable
internal fun Synopsis(
    text: String?,
    alternativeTitles: List<String>,
) {
    if (text.isNullOrBlank()) return
    val long = text.length > LONG_SYNOPSIS
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded || !long) Int.MAX_VALUE else FOLDED_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded && alternativeTitles.size > 2) {
            Text(
                alternativeTitles.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (long) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (expanded) Res.string.action_less_text else Res.string.action_more_text))
            }
        }
    }
}

internal const val LONG_SYNOPSIS = 600
internal const val FOLDED_LINES = 8
