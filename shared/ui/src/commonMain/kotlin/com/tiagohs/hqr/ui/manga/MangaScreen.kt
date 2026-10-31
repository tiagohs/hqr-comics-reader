package com.tiagohs.hqr.ui.manga

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.ui.components.DotsLoader
import com.tiagohs.hqr.ui.components.ErrorState
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.Stamp
import com.tiagohs.hqr.ui.components.label
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.downloads.rememberNotificationPermission
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_more
import com.tiagohs.hqr.ui.resources.action_open_browser
import com.tiagohs.hqr.ui.resources.action_read
import com.tiagohs.hqr.ui.resources.action_read_again
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.date_short
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.library_removed
import com.tiagohs.hqr.ui.resources.manga_add_library
import com.tiagohs.hqr.ui.resources.manga_chapter_label
import com.tiagohs.hqr.ui.resources.manga_chapters
import com.tiagohs.hqr.ui.resources.manga_clear_selection
import com.tiagohs.hqr.ui.resources.manga_continue_short
import com.tiagohs.hqr.ui.resources.manga_delete_download
import com.tiagohs.hqr.ui.resources.manga_download
import com.tiagohs.hqr.ui.resources.manga_download_next
import com.tiagohs.hqr.ui.resources.manga_download_unread
import com.tiagohs.hqr.ui.resources.manga_edit_categories
import com.tiagohs.hqr.ui.resources.manga_external
import com.tiagohs.hqr.ui.resources.manga_filter_all
import com.tiagohs.hqr.ui.resources.manga_filter_unread
import com.tiagohs.hqr.ui.resources.manga_in_library
import com.tiagohs.hqr.ui.resources.manga_mark_previous
import com.tiagohs.hqr.ui.resources.manga_mark_read
import com.tiagohs.hqr.ui.resources.manga_mark_unread
import com.tiagohs.hqr.ui.resources.manga_new
import com.tiagohs.hqr.ui.resources.manga_no_chapters
import com.tiagohs.hqr.ui.resources.manga_open_group
import com.tiagohs.hqr.ui.resources.manga_order
import com.tiagohs.hqr.ui.resources.manga_page_of
import com.tiagohs.hqr.ui.resources.manga_read
import com.tiagohs.hqr.ui.resources.manga_read_chapter
import com.tiagohs.hqr.ui.resources.manga_refresh
import com.tiagohs.hqr.ui.resources.manga_refresh_failed
import com.tiagohs.hqr.ui.resources.manga_removed_from_source
import com.tiagohs.hqr.ui.resources.manga_searching_source
import com.tiagohs.hqr.ui.resources.manga_select
import com.tiagohs.hqr.ui.resources.manga_selected
import com.tiagohs.hqr.ui.resources.migrate_action
import com.tiagohs.hqr.ui.resources.source_content_error
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrTheme
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

/** SCR-OBR-001: the work, in the colors of its cover (RF-29 to RF-33, RF-98). */
@Composable
fun MangaScreen(
    mangaId: Long,
    dark: Boolean,
    onBack: () -> Unit,
    onRead: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    onMigrate: (mangaId: Long) -> Unit = {},
    viewModel: MangaViewModel = koinViewModel(key = "manga-$mangaId") { parametersOf(mangaId) },
    prefs: AppPreferences = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coverColorOn by prefs.coverColor.collectAsStateWithLifecycle()
    val seed = state.manga?.coverColor?.takeIf { coverColorOn } ?: CoverColor.VIOLET
    HqrTheme(dark = dark, seed = seed) {
        val manga = state.manga
        if (manga == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val error = state.error
                if (error == null) {
                    LoadingState()
                } else {
                    ErrorState(
                        stringResource(Res.string.error_title),
                        stringResource(error.message()),
                        stringResource(Res.string.action_back),
                        onBack,
                    )
                }
            }
        } else {
            MangaContent(manga, state, viewModel, onBack, onRead, onMigrate)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MangaContent(
    manga: Manga,
    state: MangaViewModel.State,
    viewModel: MangaViewModel,
    onBack: () -> Unit,
    onRead: (Long, Long, Int) -> Unit,
    onMigrate: (Long) -> Unit,
) {
    val uri = LocalUriHandler.current
    val isLocal = manga.sourceId == SourceId.LOCAL
    val snackbar = remember { SnackbarHostState() }
    val categoryChoice by viewModel.categoryChoice.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val canDownload = viewModel.canDownload
    // RF-94: the first download asks for the notification permission (Android 13+); refused, the queue still runs.
    val askNotifications = rememberNotificationPermission()
    var overflow by remember { mutableStateOf(false) }
    // RF-52: taking out offers "Desfazer"; putting in asks for the categories when there are any (RF-53).
    val toggleLibrary: () -> Unit = {
        val removing = manga.inLibrary
        viewModel.toggleLibrary()
        if (!removing && categoryChoice.first.isNotEmpty()) picking = true
    }
    LaunchedEffect(viewModel) {
        viewModel.removed.collect { removal ->
            launch {
                val result =
                    snackbar.showSnackbar(
                        getString(Res.string.library_removed),
                        getString(Res.string.action_undo),
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(removal)
            }
        }
    }
    if (picking) {
        CategoryPicker(categoryChoice.first, categoryChoice.second, onDismiss = { picking = false }) {
            viewModel.setCategories(it)
            picking = false
        }
    }
    // The bar is transparent over the work's panel and takes the paper once the list scrolls under it.
    val bar = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(bar.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selected.isNotEmpty()) {
                // RF-32, RF-62: the chapters picked with a long press, together.
                TopAppBar(
                    title = { Text(stringResource(Res.string.manga_selected, selected.size)) },
                    navigationIcon = {
                        IconButton(
                            onClick = viewModel::clearSelection,
                        ) { Icon(HqrIcons.Close, stringResource(Res.string.manga_clear_selection)) }
                    },
                    actions = {
                        if (canDownload) {
                            IconButton(onClick = {
                                askNotifications()
                                viewModel.download(selected)
                            }) { Icon(HqrIcons.Download, stringResource(Res.string.manga_download)) }
                        }
                        IconButton(
                            onClick = { viewModel.setRead(selected, true) },
                        ) { Icon(HqrIcons.Check, stringResource(Res.string.manga_mark_read)) }
                        IconButton(onClick = { viewModel.setRead(selected, false) }) {
                            Icon(HqrIcons.BookmarkOutline, stringResource(Res.string.manga_mark_unread))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = { RoundIconButton(HqrIcons.Back, stringResource(Res.string.action_back), onBack) },
                    actions = {
                        Box {
                            RoundIconButton(HqrIcons.More, stringResource(Res.string.action_more)) { overflow = true }
                            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.manga_refresh)) },
                                    leadingIcon = { Icon(HqrIcons.Refresh, null) },
                                    onClick = {
                                        overflow = false
                                        viewModel.refresh()
                                    },
                                )
                                if (canDownload) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.manga_download_next, NEXT_UNREAD)) },
                                        leadingIcon = { Icon(HqrIcons.Download, null) },
                                        onClick = {
                                            overflow = false
                                            askNotifications()
                                            viewModel.downloadUnread(NEXT_UNREAD)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.manga_download_unread)) },
                                        leadingIcon = { Icon(HqrIcons.Download, null) },
                                        onClick = {
                                            overflow = false
                                            askNotifications()
                                            viewModel.downloadUnread()
                                        },
                                    )
                                }
                                if (manga.inLibrary && categoryChoice.first.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.manga_edit_categories)) },
                                        leadingIcon = { Icon(HqrIcons.Folder, null) },
                                        onClick = {
                                            overflow = false
                                            picking = true
                                        },
                                    )
                                }
                                // RF-69: local files have no other source to go to.
                                if (!isLocal) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.migrate_action)) },
                                        leadingIcon = { Icon(HqrIcons.Swap, null) },
                                        onClick = {
                                            overflow = false
                                            onMigrate(manga.id)
                                        },
                                    )
                                }
                                // RF-34: the work's page at the source.
                                manga.source.url?.takeIf { !isLocal }?.let { url ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.action_open_browser)) },
                                        leadingIcon = { Icon(HqrIcons.Globe, null) },
                                        onClick = {
                                            overflow = false
                                            uri.openUri(url)
                                        },
                                    )
                                }
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                        ),
                    scrollBehavior = bar,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing && !state.chaptersLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val sourceLabel = sourceName(manga)
            LazyColumn(Modifier.fillMaxSize()) {
                // The panel starts under the bar, so its halftone or speed lines fill the top of the screen.
                item { Hero(manga, topPadding = padding.calculateTopPadding()) }
                item { Header(manga, sourceLabel) }
                item {
                    Actions(
                        manga = manga,
                        action = state.action,
                        waiting = state.chaptersLoading,
                        onRead = { chapter, page -> onRead(manga.id, chapter.id, page) },
                        onToggleLibrary = toggleLibrary,
                    )
                }
                // RF-117: a failure of the details stays here; a failure of the list stays in the list.
                if (state.error != null && state.chaptersError == null) {
                    item {
                        InlineError(
                            stringResource(Res.string.manga_refresh_failed),
                            action = stringResource(Res.string.action_retry),
                            onAction = viewModel::refresh,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (state.chaptersLoading) {
                                stringResource(Res.string.manga_chapters, 0).filter { !it.isDigit() }.trim()
                            } else {
                                stringResource(Res.string.manga_chapters, state.totalChapters)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.chaptersLoading) {
                            // RF-117: the list says where it is still looking.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(end = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            ) {
                                DotsLoader()
                                Text(
                                    stringResource(Res.string.manga_searching_source, sourceLabel),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                viewModel.setOrder(
                                    if (state.order == MangaViewModel.Order.NEWEST_FIRST) {
                                        MangaViewModel.Order.OLDEST_FIRST
                                    } else {
                                        MangaViewModel.Order.NEWEST_FIRST
                                    },
                                )
                            }) { Icon(HqrIcons.Sort, stringResource(Res.string.manga_order)) }
                        }
                    }
                    ChapterFilters(state, viewModel)
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onSurface)
                }
                if (state.chaptersLoading) {
                    items(SKELETON_ROWS) { ChapterSkeleton(it) }
                }
                state.chaptersError?.let {
                    item {
                        InlineError(
                            stringResource(Res.string.source_content_error, sourceLabel),
                            action = stringResource(Res.string.action_retry),
                            onAction = viewModel::refresh,
                            secondary =
                                manga.source.url
                                    ?.takeIf { !isLocal }
                                    ?.let { stringResource(Res.string.action_open_browser) },
                            onSecondary = { manga.source.url?.let { uri.openUri(it) } },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                val nothingFailed = state.chaptersError == null && state.error == null
                if (state.totalChapters == 0 && !state.chaptersLoading && nothingFailed) {
                    item {
                        Text(
                            stringResource(Res.string.manga_no_chapters),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(state.chapters, key = { it.id }) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        isNew = chapter.id in state.newChapterIds,
                        download = downloadStates[chapter.id],
                        canDownload = canDownload,
                        selected = chapter.id in selected,
                        onClick = {
                            val external = chapter.source.externalUrl
                            when {
                                selected.isNotEmpty() -> viewModel.toggleSelected(chapter.id)
                                // RF-11: a chapter only on the publisher's site opens there.
                                external != null -> uri.openUri(external)
                                else -> onRead(manga.id, chapter.id, if (chapter.read) 0 else RESUME)
                            }
                        },
                        onLongPress = { viewModel.toggleSelected(chapter.id) },
                        onMarkRead = { viewModel.setRead(listOf(chapter.id), !chapter.read) },
                        onMarkPrevious = { viewModel.markPreviousAsRead(chapter) },
                        onDownload = {
                            askNotifications()
                            viewModel.download(listOf(chapter.id))
                        },
                        onDeleteDownload = { viewModel.removeDownloads(listOf(chapter.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun sourceName(manga: Manga): String =
    sourceDisplayName(
        manga.sourceId,
        koinInject<com.tiagohs.hqr.domain.source.SourceRegistry>()[manga.sourceId]?.name ?: manga.sourceId.value,
    )

/** RF-31: the main button, then the library: a round toggle in it, a full-width button out of it (RF-52). */
@Composable
private fun Actions(
    manga: Manga,
    action: ReadAction,
    waiting: Boolean,
    onRead: (Chapter, Int) -> Unit,
    onToggleLibrary: () -> Unit,
) {
    val (label, target) =
        when (action) {
            is ReadAction.Start ->
                stringResource(Res.string.manga_read_chapter, action.chapter.number.chapterLabel()) to (action.chapter to 0)
            is ReadAction.Continue ->
                stringResource(Res.string.manga_continue_short, action.chapter.number.chapterLabel()) to (action.chapter to RESUME)
            is ReadAction.ReadAgain -> stringResource(Res.string.action_read_again) to (action.chapter to 0)
            ReadAction.Nothing -> stringResource(Res.string.action_read) to null
        }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // RF-117: the button waits for the chapters; until then it cannot know where to open.
            Button(
                onClick = { target?.let { onRead(it.first, it.second) } },
                enabled = target != null && !waiting,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (manga.inLibrary) {
                androidx.compose.material3.FilledIconButton(onClick = onToggleLibrary, modifier = Modifier.size(52.dp)) {
                    Icon(HqrIcons.Bookmark, stringResource(Res.string.manga_in_library))
                }
            }
        }
        if (!manga.inLibrary) {
            androidx.compose.material3.OutlinedButton(onClick = onToggleLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(HqrIcons.Add, null, Modifier.size(18.dp))
                Text("  " + stringResource(Res.string.manga_add_library))
            }
        }
    }
}

/** RF-30: unread and scan group filters above the chapter list. */
@Composable
private fun ChapterFilters(
    state: MangaViewModel.State,
    viewModel: MangaViewModel,
) {
    if (state.totalChapters == 0) return
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        item {
            androidx.compose.material3.FilterChip(
                selected = state.filter == MangaViewModel.ChapterFilter.ALL && state.group == null,
                onClick = {
                    viewModel.setFilter(MangaViewModel.ChapterFilter.ALL)
                    viewModel.setGroup(null)
                },
                label = { Text(stringResource(Res.string.manga_filter_all)) },
            )
        }
        item {
            androidx.compose.material3.FilterChip(
                selected = state.filter == MangaViewModel.ChapterFilter.UNREAD,
                onClick = {
                    viewModel.setFilter(
                        if (state.filter ==
                            MangaViewModel.ChapterFilter.UNREAD
                        ) {
                            MangaViewModel.ChapterFilter.ALL
                        } else {
                            MangaViewModel.ChapterFilter.UNREAD
                        },
                    )
                },
                label = { Text(stringResource(Res.string.manga_filter_unread)) },
            )
        }
        if (state.groups.size > 1) {
            items(state.groups.size) { i ->
                val name = state.groups[i]
                androidx.compose.material3.FilterChip(
                    selected = state.group == name,
                    onClick = { viewModel.setGroup(if (state.group == name) null else name) },
                    label = { Text(name) },
                )
            }
        }
    }
}

@Composable
private fun shortDate(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return stringResource(
        Res.string.date_short,
        date.day.toString().padStart(2, '0'),
        date.month.number
            .toString()
            .padStart(2, '0'),
        date.year.toString(),
    )
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    isNew: Boolean,
    download: DownloadItem?,
    canDownload: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkPrevious: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val numberColor =
        when {
            chapter.read -> MaterialTheme.colorScheme.onSurfaceVariant
            chapter.inProgress -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.primary
        }
    Column(
        Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, onLongClickLabel = stringResource(Res.string.manga_select))
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .semantics { this.selected = selected },
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                chapter.number.chapterLabel(),
                fontFamily = bangers(),
                fontSize = 26.sp,
                color = numberColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    chapter.source.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.manga_chapter_label, chapter.number.chapterLabel()),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (chapter.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // RF-07: the scan group is credited and opens its page.
                val groupUrl = chapter.source.groupUrl
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                chapter.source.group?.let { group ->
                    Text(
                        group,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (groupUrl != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            if (groupUrl != null) {
                                // A real button, 48 dp tall to the finger, not a tap that also opens the chapter.
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.manga_open_group)) {
                                        uriHandler.openUri(groupUrl)
                                    }
                            } else {
                                Modifier
                            },
                    )
                }
                val meta =
                    listOfNotNull(
                        chapter.source.uploadedAtEpochMs?.let { shortDate(it) },
                        if (chapter.read) stringResource(Res.string.manga_read) else null,
                    ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (isNew) {
                    // RF-33: seen at the source after the last opening of the work.
                    Stamp(stringResource(Res.string.manga_new), Modifier.padding(top = 2.dp))
                }
                if (chapter.source.externalUrl != null) {
                    // RF-11: licensed chapters live on the publisher's site.
                    Text(
                        stringResource(Res.string.manga_external),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                if (chapter.removedAtSource) {
                    Text(
                        stringResource(Res.string.manga_removed_from_source),
                        style = MaterialTheme.typography.labelSmall,
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(6.dp),
                                ).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            if (chapter.inProgress && chapter.pageCount != null) {
                Text(
                    stringResource(Res.string.manga_page_of, chapter.lastPage + 1, chapter.pageCount!!),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DownloadMark(download)
            Box {
                IconButton(onClick = { menu = true }) { Icon(HqrIcons.More, stringResource(Res.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (chapter.read) Res.string.manga_mark_unread else Res.string.manga_mark_read)) },
                        onClick = {
                            menu = false
                            onMarkRead()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.manga_mark_previous)) },
                        onClick = {
                            menu = false
                            onMarkPrevious()
                        },
                    )
                    if (download != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.manga_delete_download)) },
                            onClick = {
                                menu = false
                                onDeleteDownload()
                            },
                        )
                    } else if (canDownload && chapter.source.externalUrl == null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.manga_download)) },
                            onClick = {
                                menu = false
                                onDownload()
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private const val NEXT_UNREAD = 5
