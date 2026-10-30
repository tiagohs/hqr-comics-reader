package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.presentation.library.LibraryDisplay
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibrarySort
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.library.TriState
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.library_badge_downloaded
import com.tiagohs.hqr.ui.resources.library_badge_source
import com.tiagohs.hqr.ui.resources.library_badge_unread
import com.tiagohs.hqr.ui.resources.library_clear_filters
import com.tiagohs.hqr.ui.resources.library_display_comfortable
import com.tiagohs.hqr.ui.resources.library_display_compact
import com.tiagohs.hqr.ui.resources.library_display_list
import com.tiagohs.hqr.ui.resources.library_filter_completed
import com.tiagohs.hqr.ui.resources.library_filter_downloaded
import com.tiagohs.hqr.ui.resources.library_filter_ongoing
import com.tiagohs.hqr.ui.resources.library_filter_started
import com.tiagohs.hqr.ui.resources.library_filter_unread
import com.tiagohs.hqr.ui.resources.library_label_category
import com.tiagohs.hqr.ui.resources.library_label_source
import com.tiagohs.hqr.ui.resources.library_label_state
import com.tiagohs.hqr.ui.resources.library_show_works
import com.tiagohs.hqr.ui.resources.library_sort_added
import com.tiagohs.hqr.ui.resources.library_sort_ascending
import com.tiagohs.hqr.ui.resources.library_sort_descending
import com.tiagohs.hqr.ui.resources.library_sort_last_read
import com.tiagohs.hqr.ui.resources.library_sort_latest_chapter
import com.tiagohs.hqr.ui.resources.library_sort_title
import com.tiagohs.hqr.ui.resources.library_sort_total
import com.tiagohs.hqr.ui.resources.library_sort_unread
import com.tiagohs.hqr.ui.resources.library_tab_display
import com.tiagohs.hqr.ui.resources.library_tab_filter
import com.tiagohs.hqr.ui.resources.library_tab_sort
import com.tiagohs.hqr.ui.resources.library_tab_uncategorized
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** SCR-LIB-002: filter, sort and display, saved as they change (RF-54, RF-55). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsSheet(
    state: LibraryViewModel.State,
    sourceNames: Map<String, String>,
    onChange: (LibraryOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val options = state.options
    ModalBottomSheet(onDismissRequest = onDismiss) {
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf(Res.string.library_tab_filter, Res.string.library_tab_sort, Res.string.library_tab_display).forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label)) })
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            when (tab) {
                0 -> FilterTab(options, state.categories, state.sourceIds, sourceNames, onChange)
                1 -> SortTab(options, onChange)
                else -> DisplayTab(options, onChange)
            }
        }
        HorizontalDivider()
        Button(
            onClick = onDismiss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .height(56.dp),
        ) { Text(stringResource(Res.string.library_show_works, state.items.size)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterTab(
    options: LibraryOptions,
    categories: List<Category>,
    sourceIds: List<String>,
    sourceNames: Map<String, String>,
    onChange: (LibraryOptions) -> Unit,
) {
    Group(Res.string.library_label_state) {
        val states =
            listOf(
                Triple(Res.string.library_filter_unread, options.unread) { v: TriState -> options.copy(unread = v) },
                Triple(Res.string.library_filter_downloaded, options.downloaded) { v: TriState -> options.copy(downloaded = v) },
                Triple(Res.string.library_filter_started, options.started) { v: TriState -> options.copy(started = v) },
                Triple(Res.string.library_filter_ongoing, options.ongoing) { v: TriState -> options.copy(ongoing = v) },
                Triple(Res.string.library_filter_completed, options.completed) { v: TriState -> options.copy(completed = v) },
            )
        states.forEach { (label, value, copy) ->
            FilterChip(
                selected = value == TriState.ONLY,
                onClick = { onChange(copy(if (value == TriState.ONLY) TriState.OFF else TriState.ONLY)) },
                label = { Text(stringResource(label)) },
            )
        }
    }
    if (sourceIds.size > 1) {
        Group(Res.string.library_label_source) {
            sourceIds.forEach { id ->
                FilterChip(
                    selected = id in options.sources,
                    onClick = { onChange(options.copy(sources = options.sources.toggle(id))) },
                    label = { Text(sourceDisplayName(SourceId(id), sourceNames[id] ?: id)) },
                )
            }
        }
    }
    if (categories.isNotEmpty()) {
        Group(Res.string.library_label_category) {
            categories.forEach { category ->
                FilterChip(
                    selected = category.id in options.categories,
                    onClick = { onChange(options.copy(categories = options.categories.toggle(category.id))) },
                    label = { Text(category.name) },
                )
            }
            FilterChip(
                selected = Category.DEFAULT_ID in options.categories,
                onClick = { onChange(options.copy(categories = options.categories.toggle(Category.DEFAULT_ID))) },
                label = { Text(stringResource(Res.string.library_tab_uncategorized)) },
            )
        }
    }
    if (options.hasFilters) {
        TextButton(onClick = { onChange(options.withoutFilters()) }, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(stringResource(Res.string.library_clear_filters))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(
    title: StringResource,
    chips: @Composable () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortTab(
    options: LibraryOptions,
    onChange: (LibraryOptions) -> Unit,
) {
    val sorts =
        listOf(
            LibrarySort.TITLE to Res.string.library_sort_title,
            LibrarySort.LAST_READ to Res.string.library_sort_last_read,
            LibrarySort.LATEST_CHAPTER to Res.string.library_sort_latest_chapter,
            LibrarySort.TOTAL_CHAPTERS to Res.string.library_sort_total,
            LibrarySort.UNREAD to Res.string.library_sort_unread,
            LibrarySort.DATE_ADDED to Res.string.library_sort_added,
        )
    sorts.forEach { (sort, label) ->
        ListItem(
            headlineContent = { Text(stringResource(label)) },
            leadingContent = { RadioButton(selected = options.sort == sort, onClick = null) },
            modifier =
                Modifier.selectable(
                    selected = options.sort == sort,
                    role = Role.RadioButton,
                ) { onChange(options.copy(sort = sort)) },
        )
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        listOf(true to Res.string.library_sort_ascending, false to Res.string.library_sort_descending).forEachIndexed { i, (up, label) ->
            SegmentedButton(
                selected = options.ascending == up,
                onClick = { onChange(options.copy(ascending = up)) },
                shape = SegmentedButtonDefaults.itemShape(i, 2),
            ) { Text(stringResource(label)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayTab(
    options: LibraryOptions,
    onChange: (LibraryOptions) -> Unit,
) {
    val modes =
        listOf(
            LibraryDisplay.COMPACT_GRID to Res.string.library_display_compact,
            LibraryDisplay.COMFORTABLE_GRID to Res.string.library_display_comfortable,
            LibraryDisplay.LIST to Res.string.library_display_list,
        )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        modes.forEachIndexed { i, (mode, label) ->
            SegmentedButton(
                selected = options.display == mode,
                onClick = { onChange(options.copy(display = mode)) },
                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
    listOf(
        Triple(Res.string.library_badge_unread, options.unreadBadge) { v: Boolean -> options.copy(unreadBadge = v) },
        Triple(Res.string.library_badge_downloaded, options.downloadedBadge) { v: Boolean -> options.copy(downloadedBadge = v) },
        Triple(Res.string.library_badge_source, options.sourceBadge) { v: Boolean -> options.copy(sourceBadge = v) },
    ).forEach { (label, on, copy) ->
        ListItem(
            headlineContent = { Text(stringResource(label)) },
            trailingContent = { Switch(checked = on, onCheckedChange = null) },
            modifier = Modifier.toggleable(value = on, role = Role.Switch) { onChange(copy(it)) },
        )
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
