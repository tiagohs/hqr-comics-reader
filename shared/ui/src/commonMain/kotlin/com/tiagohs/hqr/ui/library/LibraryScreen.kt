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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.library.LibraryDisplay
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.HalftoneProgress
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.UnreadBalloon
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_continue
import com.tiagohs.hqr.ui.resources.action_more
import com.tiagohs.hqr.ui.resources.library_categories
import com.tiagohs.hqr.ui.resources.library_clear_filters
import com.tiagohs.hqr.ui.resources.library_continue_reading
import com.tiagohs.hqr.ui.resources.library_empty_message
import com.tiagohs.hqr.ui.resources.library_empty_title
import com.tiagohs.hqr.ui.resources.library_filter_downloaded
import com.tiagohs.hqr.ui.resources.library_filtered_message
import com.tiagohs.hqr.ui.resources.library_filtered_title
import com.tiagohs.hqr.ui.resources.library_options
import com.tiagohs.hqr.ui.resources.library_search_hint
import com.tiagohs.hqr.ui.resources.library_tab_all
import com.tiagohs.hqr.ui.resources.library_tab_uncategorized
import com.tiagohs.hqr.ui.resources.library_unread
import com.tiagohs.hqr.ui.resources.library_up_to_date
import com.tiagohs.hqr.ui.resources.manga_continue_chapter
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.nav_library
import com.tiagohs.hqr.ui.resources.onomatopoeia_empty
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrShapes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** SCR-LIB-001: "Continuar lendo", category tabs, search and the grid or list (RF-52 to RF-56). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenManga: (Long) -> Unit,
    onContinue: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    onExplore: () -> Unit,
    onCategories: () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
    registry: SourceRegistry = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            if (searching) {
                TopAppBar(
                    title = {
                        // RF-56: title, author and genre.
                        TextField(
                            value = state.query,
                            onValueChange = viewModel::search,
                            placeholder = { Text(stringResource(Res.string.library_search_hint)) },
                            singleLine = true,
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.search("")
                            searching = false
                        }) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(Res.string.nav_library), style = MaterialTheme.typography.headlineLarge) },
                    actions = {
                        if (!state.isEmpty) {
                            IconButton(
                                onClick = { searching = true },
                            ) { Icon(HqrIcons.Search, stringResource(Res.string.library_search_hint)) }
                            IconButton(onClick = { optionsOpen = true }) {
                                BadgedBox(badge = { if (state.options.hasFilters) Badge() }) {
                                    Icon(HqrIcons.Filter, stringResource(Res.string.library_options))
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(HqrIcons.More, stringResource(Res.string.action_more)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.library_categories)) },
                                    onClick = {
                                        menu = false
                                        onCategories()
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.isEmpty ->
                EmptyState(
                    title = stringResource(Res.string.library_empty_title),
                    message = stringResource(Res.string.library_empty_message),
                    onomatopoeia = stringResource(Res.string.onomatopoeia_empty),
                    action = stringResource(Res.string.nav_explore),
                    onAction = onExplore,
                    modifier = Modifier.padding(padding),
                )
            else ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    if (state.tabs.size > 1) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = state.tabs.indexOfFirst { it.id == state.selectedTab }.coerceAtLeast(0),
                            edgePadding = 8.dp,
                        ) {
                            state.tabs.forEach { tab ->
                                Tab(
                                    selected = tab.id == state.selectedTab,
                                    onClick = { viewModel.selectTab(tab.id) },
                                    text = {
                                        val name =
                                            when (tab.id) {
                                                LibraryViewModel.ALL -> stringResource(Res.string.library_tab_all)
                                                LibraryViewModel.UNCATEGORIZED -> stringResource(Res.string.library_tab_uncategorized)
                                                else -> tab.name
                                            }
                                        Text("$name ${tab.count}")
                                    },
                                )
                            }
                        }
                    }
                    if (state.isFilteredOut) {
                        EmptyState(
                            title = stringResource(Res.string.library_filtered_title),
                            message = stringResource(Res.string.library_filtered_message),
                            action = if (state.options.hasFilters) stringResource(Res.string.library_clear_filters) else null,
                            onAction = { viewModel.setOptions(state.options.withoutFilters()) },
                        )
                    } else {
                        LibraryItems(
                            state.items,
                            state.options,
                            if (state.query.isBlank()) state.continueReading else null,
                            onOpenManga,
                            onContinue,
                        )
                    }
                }
        }
    }
    if (optionsOpen) {
        val names = registry.everything.associate { it.id.value to it.name }
        LibraryOptionsSheet(state, names, viewModel::setOptions) { optionsOpen = false }
    }
}

@Composable
private fun LibraryItems(
    items: List<LibraryItem>,
    options: LibraryOptions,
    continueReading: HistoryEntry?,
    onOpenManga: (Long) -> Unit,
    onContinue: (Long, Long, Int) -> Unit,
) {
    // RF-54: the number of columns follows the width; the list is one per row.
    val columns =
        when (options.display) {
            LibraryDisplay.COMPACT_GRID -> GridCells.Adaptive(COMPACT_CELL)
            LibraryDisplay.COMFORTABLE_GRID -> GridCells.Adaptive(COMFORTABLE_CELL)
            LibraryDisplay.LIST -> GridCells.Fixed(1)
        }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(if (options.display == LibraryDisplay.LIST) 8.dp else 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (continueReading != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { ContinueCard(continueReading, onContinue) }
        }
        items(items, key = { it.manga.id }) { item ->
            val unreadLabel =
                if (item.unread > 0) {
                    pluralStringResource(Res.plurals.library_unread, item.unread, item.unread)
                } else {
                    stringResource(Res.string.library_up_to_date)
                }
            val downloadedLabel = if (item.downloaded > 0) ", " + stringResource(Res.string.library_filter_downloaded).lowercase() else ""
            val description = "${item.manga.title}, $unreadLabel$downloadedLabel"
            when (options.display) {
                LibraryDisplay.LIST ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenManga(item.manga.id) }
                            .semantics(mergeDescendants = true) { contentDescription = description },
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(LIST_COVER)) { Cover(item.manga.source, shape = HqrShapes.coverSmall) { Badges(item, options) } }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                item.manga.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                unreadLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                LibraryDisplay.COMPACT_GRID ->
                    Box(
                        Modifier
                            .clickable { onOpenManga(item.manga.id) }
                            .semantics(mergeDescendants = true) { contentDescription = description },
                    ) {
                        Cover(item.manga.source) {
                            Badges(item, options)
                            // The title sits on the cover, over a shade that keeps it readable.
                            Text(
                                item.manga.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = SHADE))))
                                        .padding(start = 8.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
                            )
                        }
                    }
                LibraryDisplay.COMFORTABLE_GRID ->
                    Column(
                        Modifier
                            .clickable { onOpenManga(item.manga.id) }
                            .semantics(mergeDescendants = true) { contentDescription = description },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Cover(item.manga.source) { Badges(item, options) }
                        Text(item.manga.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(unreadLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
            }
        }
    }
}

/** RF-54: unread, downloaded and source badges, each one can be turned off. */
@Composable
private fun BoxScope.Badges(
    item: LibraryItem,
    options: LibraryOptions,
) {
    if (options.unreadBadge) UnreadBalloon(item.unread)
    Column(Modifier.align(Alignment.TopStart).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (options.downloadedBadge && item.downloaded > 0) {
            Icon(
                HqrIcons.Downloaded,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(6.dp),
                        ).padding(3.dp)
                        .size(14.dp),
            )
        }
        if (options.sourceBadge) {
            Text(
                sourceDisplayName(item.manga.sourceId, item.manga.sourceId.value).take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(6.dp),
                        ).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

private val COMPACT_CELL = 96.dp
private val COMFORTABLE_CELL = 108.dp
private val LIST_COVER = 56.dp
private const val SHADE = 0.7f

@Composable
private fun ContinueCard(
    entry: HistoryEntry,
    onContinue: (Long, Long, Int) -> Unit,
) {
    val chapter = entry.chapter
    val total = chapter.pageCount ?: 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(68.dp)) { Cover(entry.manga.source, shape = HqrShapes.coverSmall) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.library_continue_reading),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(entry.manga.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(Res.string.manga_continue_chapter, chapter.number.chapterLabel(), chapter.lastPage + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (total > 0) HalftoneProgress((chapter.lastPage + 1f) / total)
        }
        Button(onClick = { onContinue(entry.manga.id, chapter.id, RESUME) }) { Text(stringResource(Res.string.action_continue)) }
    }
}
