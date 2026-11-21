package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.presentation.library.LibraryProgress
import com.tiagohs.hqr.presentation.library.ReadingState
import com.tiagohs.hqr.ui.components.CoverImage
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_continue
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.library_characters
import com.tiagohs.hqr.ui.resources.library_characters_add
import com.tiagohs.hqr.ui.resources.library_characters_all
import com.tiagohs.hqr.ui.resources.library_characters_edit
import com.tiagohs.hqr.ui.resources.library_characters_hint
import com.tiagohs.hqr.ui.resources.library_characters_new
import com.tiagohs.hqr.ui.resources.library_characters_remove
import com.tiagohs.hqr.ui.resources.library_characters_title
import com.tiagohs.hqr.ui.resources.library_continue
import com.tiagohs.hqr.ui.resources.library_new_count
import com.tiagohs.hqr.ui.resources.library_new_line
import com.tiagohs.hqr.ui.resources.library_reading_corner
import com.tiagohs.hqr.ui.resources.library_reading_number
import com.tiagohs.hqr.ui.resources.library_state_all
import com.tiagohs.hqr.ui.resources.library_state_new
import com.tiagohs.hqr.ui.resources.library_times_read
import com.tiagohs.hqr.ui.resources.manga_continue_chapter
import com.tiagohs.hqr.ui.resources.read_again_confirm
import com.tiagohs.hqr.ui.resources.read_again_just_open
import com.tiagohs.hqr.ui.resources.read_again_message
import com.tiagohs.hqr.ui.resources.read_again_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource

/** RF-133: the last readings of the area as a Material hero carousel; the big item resumes in one tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueCarousel(
    entries: List<HistoryEntry>,
    onContinue: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    Column(modifier) {
        SectionTitle(stringResource(Res.string.library_continue))
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { entries.size },
            preferredItemWidth = CAROUSEL_ITEM,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(CAROUSEL_HEIGHT),
        ) { i ->
            val entry = entries[i]
            val chapter = entry.chapter
            val line = stringResource(Res.string.manga_continue_chapter, chapter.number.chapterLabel(), chapter.lastPage + 1)
            val again =
                if (entry.manga.isRereading) {
                    stringResource(
                        Res.string.library_reading_number,
                        entry.manga.timesRead + 1,
                    ) + " · "
                } else {
                    ""
                }
            val resume = stringResource(Res.string.action_continue)
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable(onClickLabel = resume) { onContinue(entry.manga.id, chapter.id, RESUME) }
                    .semantics(mergeDescendants = true) { contentDescription = "${entry.manga.title}, $again$line" },
            ) {
                CoverImage(entry.manga.source)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(SCRIM_STOPS)))
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 72.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        entry.manga.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PAPER,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("$again$line", style = MaterialTheme.typography.labelMedium, color = PAPER, maxLines = 1)
                }
                FilledIconButton(
                    onClick = { onContinue(entry.manga.id, chapter.id, RESUME) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Icon(HqrIcons.Play, resume) }
                val total = chapter.pageCount ?: 0
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { (chapter.lastPage + 1f) / total },
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                }
            }
        }
    }
}

/** RF-132: works whose source added chapters after a full reading, with how many came out. */
@Composable
fun NewChaptersSection(
    items: List<LibraryItem>,
    onOpen: (Long) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier) {
        SectionTitle(stringResource(Res.string.library_state_new), stringResource(Res.string.library_state_all) to onSeeAll)
        Surface(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                items.take(NEW_LIMIT).forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.manga.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(stringResource(Res.string.library_new_line, item.unread)) },
                        leadingContent = {
                            Box(Modifier.width(44.dp).height(64.dp).clip(MaterialTheme.shapes.small)) { CoverImage(item.manga.source) }
                        },
                        trailingContent = { Pill(stringResource(Res.string.library_new_count, item.unread), STATE_NEW, INK) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onOpen(item.manga.id) },
                    )
                }
            }
        }
    }
}

/** RF-134: the characters found in the HQs; one at a time filters the whole area. */
@Composable
fun CharacterChips(
    characters: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionTitle(
            stringResource(Res.string.library_characters),
            stringResource(Res.string.library_characters_edit) to onEdit,
            small = true,
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(Res.string.library_characters_all)) },
                    leadingIcon =
                        if (selected ==
                            null
                        ) {
                            ({ Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) })
                        } else {
                            null
                        },
                )
            }
            items(characters, key = { it.first }) { (name, count) ->
                val on = selected == name
                FilterChip(
                    selected = on,
                    onClick = { onSelect(if (on) null else name) },
                    label = { Text("$name $count") },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(FilterChipDefaults.IconSize + 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                name.take(1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }
        }
    }
}

/** RF-134: the list of characters, to add or take out. */
@Composable
fun CharactersDialog(
    names: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.library_characters_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.library_characters_hint), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(Res.string.library_characters_new)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        onAdd(text)
                        text = ""
                    }, enabled = text.isNotBlank()) { Text(stringResource(Res.string.library_characters_add)) }
                }
                names.sorted().forEach { name ->
                    ListItem(
                        headlineContent = { Text(name) },
                        trailingContent = {
                            IconButton(onClick = { onRemove(name) }) {
                                Icon(HqrIcons.Close, stringResource(Res.string.library_characters_remove, name))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_ok)) } },
    )
}

/** RF-135: a chapter of a work read to the end: start a new reading or only open it. */
@Composable
fun ReadAgainDialog(
    nextReading: Int,
    onReadAgain: () -> Unit,
    onJustOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(HqrIcons.Refresh, null) },
        title = { Text(stringResource(Res.string.read_again_title)) },
        text = { Text(stringResource(Res.string.read_again_message, nextReading)) },
        confirmButton = { TextButton(onClick = onReadAgain) { Text(stringResource(Res.string.read_again_confirm)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
                TextButton(onClick = onJustOpen) { Text(stringResource(Res.string.read_again_just_open)) }
            }
        },
    )
}

/** RF-132/RF-135: the mark at the corner of a cover: % read, "+3", ✓, "2×" or "2ª · 28%". */
@Composable
fun BoxScope.StateCorner(item: LibraryItem) {
    val state = LibraryProgress.state(item)
    val total = item.totalChapters
    val percent = if (total == 0) 0 else LibraryProgress.chaptersRead(item) * PERCENT_INT / total
    val modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 12.dp)
    val again = LibraryProgress.readingNumber(item)
    when {
        again != null -> {
            val at = ((item.manga.rereadChapter ?: 0.0) * PERCENT_INT / total.coerceAtLeast(1)).toInt().coerceIn(0, PERCENT_INT)
            Pill(stringResource(Res.string.library_reading_corner, again, "$at%"), CORNER_DARK, PAPER, modifier)
        }
        state == ReadingState.NEW_CHAPTERS -> Pill(stringResource(Res.string.library_new_count, item.unread), STATE_NEW, INK, modifier)
        state == ReadingState.UP_TO_DATE || state == ReadingState.FINISHED -> {
            val times = LibraryProgress.timesFinished(item)
            val bg = if (state == ReadingState.FINISHED) MaterialTheme.colorScheme.primary else STATE_UP_TO_DATE
            val fg = if (state == ReadingState.FINISHED) MaterialTheme.colorScheme.onPrimary else STATE_UP_TO_DATE_INK
            if (times != null) {
                Pill(stringResource(Res.string.library_times_read, times), bg, fg, modifier)
            } else {
                Box(modifier.size(24.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                    Icon(HqrIcons.Check, null, Modifier.size(14.dp), tint = fg)
                }
            }
        }
        state == ReadingState.READING && percent > 0 -> Pill("$percent%", CORNER_DARK, PAPER, modifier)
        else -> Unit
    }
}

@Composable
private fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = content,
        maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(background).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionTitle(
    text: String,
    action: Pair<String, () -> Unit>? = null,
    small: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = if (small) 4.dp else 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            color = if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}
