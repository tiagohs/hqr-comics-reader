package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.model.Shelf
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.presentation.library.LibraryArea
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.library_area_all
import com.tiagohs.hqr.ui.resources.library_area_comics
import com.tiagohs.hqr.ui.resources.library_area_label
import com.tiagohs.hqr.ui.resources.library_area_manga
import com.tiagohs.hqr.ui.resources.mark_many_none
import com.tiagohs.hqr.ui.resources.mark_many_none_hint
import com.tiagohs.hqr.ui.resources.mark_many_one_hint
import com.tiagohs.hqr.ui.resources.mark_many_read
import com.tiagohs.hqr.ui.resources.mark_many_read_hint
import com.tiagohs.hqr.ui.resources.mark_many_title
import com.tiagohs.hqr.ui.resources.shelf_follow
import com.tiagohs.hqr.ui.resources.shelf_follow_hint
import com.tiagohs.hqr.ui.resources.shelf_hint
import com.tiagohs.hqr.ui.resources.shelf_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** RF-128: "Tudo", "HQs" and "Mangás" with their counts, above the category tabs. */
@Composable
fun LibraryAreaRow(
    area: LibraryArea,
    counts: Map<LibraryArea, Int>,
    onSelect: (LibraryArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.library_area_label)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().semantics { contentDescription = label }) {
        LibraryArea.entries.forEachIndexed { i, value ->
            SegmentedButton(
                selected = area == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, LibraryArea.entries.size),
            ) { Text("${stringResource(value.label())} ${counts[value] ?: 0}", maxLines = 1) }
        }
    }
}

fun LibraryArea.label(): StringResource =
    when (this) {
        LibraryArea.ALL -> Res.string.library_area_all
        LibraryArea.COMICS -> Res.string.library_area_comics
        LibraryArea.MANGA -> Res.string.library_area_manga
    }

/** Chapters read of a work in the library: total minus unread. */
val LibraryItem.readCount: Int get() = (totalChapters - unread).coerceAtLeast(0)

/** RF-129: the bar at the foot of a cover while a work is partly read. */
fun LibraryItem.showsReadBar(): Boolean = totalChapters > 0 && readCount in 1 until totalChapters

/** RF-129: "LIDA!" on a work read to the end, the bar at its foot while it is partly read. */
@Composable
fun BoxScope.ReadingOnCover(item: LibraryItem) {
    val total = item.totalChapters
    if (total == 0) return
    // RF-132: the corner says the situation (✓, "2×", "+3", "47%"); the bar shows how far.
    StateCorner(item)
    if (item.showsReadBar()) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(READ_BAR)
                .background(BAR_TRACK),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(item.readCount.toFloat() / total)
                    .background(MaterialTheme.colorScheme.inversePrimary),
            )
        }
    }
}

/** RF-126: several works at once: all read, or none. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkManySheet(
    count: Int,
    onDismiss: () -> Unit,
    onMark: (read: Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(Res.string.mark_many_title, count),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).semantics { heading() },
            )
            val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ListItem(
                headlineContent = { Text(stringResource(Res.string.mark_many_read)) },
                supportingContent = { Text(stringResource(Res.string.mark_many_read_hint)) },
                leadingContent = { Icon(HqrIcons.MarkRead, null, tint = MaterialTheme.colorScheme.primary) },
                colors = colors,
                modifier = Modifier.clickable { onMark(true) },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.mark_many_none)) },
                supportingContent = { Text(stringResource(Res.string.mark_many_none_hint)) },
                leadingContent = { Icon(HqrIcons.MarkUnread, null, tint = MaterialTheme.colorScheme.primary) },
                colors = colors,
                modifier = Modifier.clickable { onMark(false) },
            )
            Text(
                stringResource(Res.string.mark_many_one_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

private val READ_BAR = 6.dp
private val BAR_TRACK = Color(0x8C000000)

/** RF-128: "Onde fica na biblioteca" for one work: like its source, HQs or Mangás. */
@Composable
fun ShelfDialog(
    title: String,
    sourceName: String,
    sourceShelf: Shelf,
    current: Shelf?,
    onDismiss: () -> Unit,
    onChoose: (Shelf?) -> Unit,
) {
    var choice by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.shelf_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                Text(stringResource(Res.string.shelf_hint, title), style = MaterialTheme.typography.bodyMedium)
                val options =
                    listOf(
                        null to (
                            stringResource(Res.string.shelf_follow) to
                                stringResource(Res.string.shelf_follow_hint, sourceName, stringResource(sourceShelf.area().label()))
                        ),
                        Shelf.COMICS to (stringResource(Res.string.library_area_comics) to null),
                        Shelf.MANGA to (stringResource(Res.string.library_area_manga) to null),
                    )
                options.forEach { (value, texts) ->
                    ListItem(
                        headlineContent = { Text(texts.first) },
                        supportingContent = texts.second?.let { { Text(it) } },
                        leadingContent = { RadioButton(selected = choice == value, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.selectable(selected = choice == value, role = Role.RadioButton) { choice = value },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onChoose(choice) }) { Text(stringResource(Res.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

fun Shelf.area(): LibraryArea = if (this == Shelf.MANGA) LibraryArea.MANGA else LibraryArea.COMICS
