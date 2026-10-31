package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.library.LibraryDisplay
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.ui.components.ComicTitle
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.HalftoneProgress
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.UnreadBalloon
import com.tiagohs.hqr.ui.components.cascadeIn
import com.tiagohs.hqr.ui.components.rememberOpening
import com.tiagohs.hqr.ui.components.sharedCover
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.components.unavailableSourceName
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_continue
import com.tiagohs.hqr.ui.resources.action_more
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.downloads_title
import com.tiagohs.hqr.ui.resources.library_categories
import com.tiagohs.hqr.ui.resources.library_clear_filters
import com.tiagohs.hqr.ui.resources.library_clear_selection
import com.tiagohs.hqr.ui.resources.library_continue_reading
import com.tiagohs.hqr.ui.resources.library_download_unread
import com.tiagohs.hqr.ui.resources.library_empty_message
import com.tiagohs.hqr.ui.resources.library_empty_title
import com.tiagohs.hqr.ui.resources.library_filter_downloaded
import com.tiagohs.hqr.ui.resources.library_filtered_message
import com.tiagohs.hqr.ui.resources.library_filtered_title
import com.tiagohs.hqr.ui.resources.library_mark_read
import com.tiagohs.hqr.ui.resources.library_move
import com.tiagohs.hqr.ui.resources.library_new_many
import com.tiagohs.hqr.ui.resources.library_new_none
import com.tiagohs.hqr.ui.resources.library_new_one
import com.tiagohs.hqr.ui.resources.library_options
import com.tiagohs.hqr.ui.resources.library_picked
import com.tiagohs.hqr.ui.resources.library_remove_picked
import com.tiagohs.hqr.ui.resources.library_removed_many
import com.tiagohs.hqr.ui.resources.library_search_hint
import com.tiagohs.hqr.ui.resources.library_select_all
import com.tiagohs.hqr.ui.resources.library_tab_all
import com.tiagohs.hqr.ui.resources.library_tab_uncategorized
import com.tiagohs.hqr.ui.resources.library_unread
import com.tiagohs.hqr.ui.resources.library_up_to_date
import com.tiagohs.hqr.ui.resources.library_update_failed
import com.tiagohs.hqr.ui.resources.library_updating
import com.tiagohs.hqr.ui.resources.manga_continue_chapter
import com.tiagohs.hqr.ui.resources.manga_select
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.nav_library
import com.tiagohs.hqr.ui.resources.onomatopoeia_empty
import com.tiagohs.hqr.ui.resources.source_unavailable
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrShapes
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
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
    onDownloads: () -> Unit = {},
    viewModel: LibraryViewModel = koinViewModel(),
    registry: SourceRegistry = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Downloads have a shortcut here, with a badge while the queue runs (UX §4); only where downloads exist.
    val downloads = getKoin().getOrNull<Downloads>()
    val queue by (downloads?.observe() ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val running = queue.any { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED }
    var searching by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val picked by viewModel.picked.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val updated by viewModel.updated.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var moving by remember { mutableStateOf(false) }
    // RF-58: leaving the app stops the update; what arrived stays.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopUpdate() }
    updated?.let { result ->
        val text =
            when (result.newChapters) {
                0 -> stringResource(Res.string.library_new_none)
                1 -> stringResource(Res.string.library_new_one)
                else -> stringResource(Res.string.library_new_many, result.newChapters)
            } + if (result.failed > 0) " · " + stringResource(Res.string.library_update_failed, result.failed) else ""
        LaunchedEffect(result) {
            viewModel.updateShown()
            snackbar.showSnackbar(text)
        }
    }
    if (moving) {
        MoveDialog(state.categories, onDismiss = { moving = false }) {
            moving = false
            viewModel.moveSelected(it)
        }
    }
    val removedText = stringResource(Res.string.library_removed_many, picked.size)
    val undoText = stringResource(Res.string.action_undo)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (picked.isNotEmpty()) {
                // RF-57: what can be done with the picked works.
                TopAppBar(
                    title = { Text(stringResource(Res.string.library_picked, picked.size)) },
                    navigationIcon = {
                        IconButton(
                            onClick = viewModel::clearSelection,
                        ) { Icon(HqrIcons.Close, stringResource(Res.string.library_clear_selection)) }
                    },
                    actions = {
                        IconButton(onClick = viewModel::selectAll) { Icon(HqrIcons.Library, stringResource(Res.string.library_select_all)) }
                        if (state.categories.isNotEmpty()) {
                            IconButton(onClick = { moving = true }) { Icon(HqrIcons.Folder, stringResource(Res.string.library_move)) }
                        }
                        IconButton(
                            onClick = viewModel::markSelectedRead,
                        ) { Icon(HqrIcons.Check, stringResource(Res.string.library_mark_read)) }
                        if (downloads != null) {
                            IconButton(onClick = viewModel::downloadSelectedUnread) {
                                Icon(HqrIcons.Download, stringResource(Res.string.library_download_unread))
                            }
                        }
                        IconButton(onClick = {
                            viewModel.removeSelected { removals ->
                                scope.launch {
                                    val result = snackbar.showSnackbar(removedText, undoText, duration = SnackbarDuration.Long)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemoveAll(removals)
                                }
                            }
                        }) { Icon(HqrIcons.Delete, stringResource(Res.string.library_remove_picked)) }
                    },
                )
            } else if (searching) {
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
                        if (downloads != null) {
                            IconButton(onClick = onDownloads) {
                                BadgedBox(badge = { if (running) Badge() }) {
                                    Icon(HqrIcons.Download, stringResource(Res.string.downloads_title))
                                }
                            }
                        }
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
                PullToRefreshBox(
                    isRefreshing = update != null,
                    onRefresh = viewModel::updateLibrary,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // RF-58: where the update is, work by work.
                        update?.let { progress ->
                            Text(
                                stringResource(
                                    Res.string.library_updating,
                                    (progress.done + 1).coerceAtMost(progress.total),
                                    progress.total,
                                    progress.current.orEmpty(),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).semantics {
                                        liveRegion =
                                            LiveRegionMode.Polite
                                    },
                            )
                            LinearProgressIndicator(
                                progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
                                if (state.query.isBlank() && picked.isEmpty()) state.continueReading else null,
                                onOpenManga,
                                onContinue,
                                picked,
                                viewModel::toggleSelected,
                            )
                        }
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
    picked: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    val selectLabel = stringResource(Res.string.manga_select)

    // RF-57: a long press picks a work; while some are picked, a tap picks or unpicks.
    @Composable
    fun Modifier.pickable(id: Long): Modifier =
        combinedClickable(
            onClick = { if (picked.isNotEmpty()) onToggle(id) else onOpenManga(id) },
            onLongClick = { onToggle(id) },
            onLongClickLabel = selectLabel,
        ).then(
            if (id in picked) {
                Modifier
                    .border(4.dp, MaterialTheme.colorScheme.primary)
                    .padding(4.dp)
            } else {
                Modifier
            },
        )
    // RF-54: the number of columns follows the width; the list is one per row.
    val columns =
        when (options.display) {
            LibraryDisplay.COMPACT_GRID -> GridCells.Adaptive(COMPACT_CELL)
            LibraryDisplay.COMFORTABLE_GRID -> GridCells.Adaptive(COMFORTABLE_CELL)
            LibraryDisplay.LIST -> GridCells.Fixed(1)
        }
    // RF-118: the works cascade in as the library opens.
    val opening = rememberOpening()
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
        itemsIndexed(items, key = { _, work -> work.manga.id }) { index, item ->
            val cascade = Modifier.cascadeIn(index, opening)
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
                        cascade
                            .fillMaxWidth()
                            .pickable(item.manga.id)
                            .semantics(mergeDescendants = true) {
                                contentDescription = description
                                selected = item.manga.id in picked
                            },
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(LIST_COVER)) {
                            Cover(
                                item.manga.source,
                                Modifier.sharedCover(item.manga.source),
                                shape = HqrShapes.coverSmall,
                            ) { Badges(item, options) }
                        }
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
                        cascade
                            .pickable(item.manga.id)
                            .semantics(mergeDescendants = true) {
                                contentDescription = description
                                selected = item.manga.id in picked
                            },
                    ) {
                        Cover(item.manga.source, Modifier.sharedCover(item.manga.source)) {
                            Badges(item, options)
                            // ADR-026: the title in a yellow caption box, like a comic panel's narration.
                            Text(
                                item.manga.title,
                                fontFamily = bangers(),
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                letterSpacing = 0.5.sp,
                                color = CAPTION_INK,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth(CAPTION_WIDTH)
                                        .background(CAPTION)
                                        .border(2.dp, CAPTION_INK)
                                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                LibraryDisplay.COMFORTABLE_GRID ->
                    Column(
                        cascade
                            .pickable(item.manga.id)
                            .semantics(mergeDescendants = true) {
                                contentDescription = description
                                selected = item.manga.id in picked
                            },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Cover(item.manga.source, Modifier.sharedCover(item.manga.source)) { Badges(item, options) }
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
        // RF-73: always shown, whatever the badge options: the work can't be refreshed or read online.
        unavailableSourceName(item.manga.sourceId)?.let { name ->
            Icon(
                HqrIcons.Warning,
                stringResource(Res.string.source_unavailable, name),
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(6.dp))
                        .padding(3.dp)
                        .size(14.dp),
            )
        }
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
private const val CAPTION_WIDTH = 0.88f
private val CAPTION = Color(0xFFFFF1A8)
private val CAPTION_INK = Color(0xFF16161A)

/** "Continuar lendo" as a speech balloon: an ink-framed panel with a shadow and the tail below (ADR-026). */
@Composable
private fun ContinueCard(
    entry: HistoryEntry,
    onContinue: (Long, Long, Int) -> Unit,
) {
    val chapter = entry.chapter
    val total = chapter.pageCount ?: 0
    val ink = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(ink, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = size) }
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(3.dp, ink)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(60.dp)) { Cover(entry.manga.source, shape = HqrShapes.coverSmall) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.library_continue_reading),
                    fontFamily = bangers(),
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ComicTitle(entry.manga.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2)
                Text(
                    stringResource(Res.string.manga_continue_chapter, chapter.number.chapterLabel(), chapter.lastPage + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (total > 0) HalftoneProgress((chapter.lastPage + 1f) / total)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.Top) {
            // The balloon's tail.
            Box(
                Modifier
                    .size(18.dp)
                    .drawBehind {
                        val path =
                            androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(0f, size.height)
                                close()
                            }
                        drawPath(path, ink)
                    },
            )
            Box(Modifier.weight(1f))
            Button(
                onClick = { onContinue(entry.manga.id, chapter.id, RESUME) },
                modifier = Modifier.offset(y = (-20).dp),
            ) { Text(stringResource(Res.string.action_continue)) }
        }
    }
}

/** RF-57: the categories the picked works go to (all of them replace what each had). */
@Composable
private fun MoveDialog(
    categories: List<com.tiagohs.hqr.domain.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    var chosen by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.library_move)) },
        text = {
            Column {
                categories.forEach { category ->
                    val on = category.id in chosen
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = on,
                                role = Role.Checkbox,
                            ) { chosen = if (it) chosen + category.id else chosen - category.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = on, onCheckedChange = null)
                        Text(category.name, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen) }) { Text(stringResource(Res.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
