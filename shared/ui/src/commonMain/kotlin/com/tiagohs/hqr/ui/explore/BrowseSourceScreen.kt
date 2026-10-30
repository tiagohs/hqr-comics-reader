package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.SavedSearch
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel.Listing
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.sources.local.ImportedFiles
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.ErrorState
import com.tiagohs.hqr.ui.components.InLibraryBadge
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.importMessage
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_choose_folder
import com.tiagohs.hqr.ui.resources.action_more
import com.tiagohs.hqr.ui.resources.action_open_browser
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.action_save
import com.tiagohs.hqr.ui.resources.action_search
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.bridge_configure
import com.tiagohs.hqr.ui.resources.bridge_needed
import com.tiagohs.hqr.ui.resources.browse_added
import com.tiagohs.hqr.ui.resources.browse_in_library
import com.tiagohs.hqr.ui.resources.browse_latest
import com.tiagohs.hqr.ui.resources.browse_no_results
import com.tiagohs.hqr.ui.resources.browse_no_results_message
import com.tiagohs.hqr.ui.resources.browse_popular
import com.tiagohs.hqr.ui.resources.challenge_hint
import com.tiagohs.hqr.ui.resources.challenge_verify
import com.tiagohs.hqr.ui.resources.error_bridge
import com.tiagohs.hqr.ui.resources.error_challenge
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.feed_add
import com.tiagohs.hqr.ui.resources.feed_add_latest
import com.tiagohs.hqr.ui.resources.feed_added
import com.tiagohs.hqr.ui.resources.feed_full
import com.tiagohs.hqr.ui.resources.files_empty_message
import com.tiagohs.hqr.ui.resources.files_empty_title
import com.tiagohs.hqr.ui.resources.files_import
import com.tiagohs.hqr.ui.resources.filter_active
import com.tiagohs.hqr.ui.resources.filter_title
import com.tiagohs.hqr.ui.resources.library_removed
import com.tiagohs.hqr.ui.resources.local_no_folder
import com.tiagohs.hqr.ui.resources.local_no_folder_message
import com.tiagohs.hqr.ui.resources.manga_add_library
import com.tiagohs.hqr.ui.resources.manga_remove_library
import com.tiagohs.hqr.ui.resources.saved_delete
import com.tiagohs.hqr.ui.resources.saved_name
import com.tiagohs.hqr.ui.resources.saved_options
import com.tiagohs.hqr.ui.resources.saved_rename
import com.tiagohs.hqr.ui.resources.saved_save
import com.tiagohs.hqr.ui.resources.saved_title
import com.tiagohs.hqr.ui.settings.rememberFolderPicker
import com.tiagohs.hqr.ui.theme.HqrIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** SCR-EXP-002: Populares, Recentes and search of one source, with continuous loading (RF-21, RF-22). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSourceScreen(
    sourceId: String,
    onBack: () -> Unit,
    onOpenManga: (Long) -> Unit,
    initialQuery: String? = null,
    onBridge: () -> Unit = {},
    onChallenge: (url: String) -> Unit = {},
    viewModel: BrowseSourceViewModel =
        koinViewModel(key = "browse-$sourceId-${initialQuery.orEmpty()}") { parametersOf(SourceId(sourceId), initialQuery) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // RF-106: back from the verification, the source is asked again — once, only after the verification was opened.
    var verifying by rememberSaveable { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (verifying) {
            verifying = false
            // After the process was killed the source is loading anew: no second request.
            if (viewModel.state.value.error == ErrorKind.CHALLENGE) viewModel.retry()
        }
        onPauseOrDispose {}
    }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf(state.query) }
    val grid = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            (
                grid.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            ) >=
                grid.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) viewModel.loadMore() }
    val isLocal = sourceId == SourceId.LOCAL.value
    val pickFolder = rememberFolderPicker { if (it != null) viewModel.retry() }
    // RF-110: in the browser the files are imported; a new import (or a drop on the window) reloads the list.
    val imported = if (isLocal) getKoin().getOrNull<ImportedFiles>() else null
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    if (imported != null) {
        val changes by imported.changes.collectAsStateWithLifecycle()
        LaunchedEffect(changes) { if (changes > 0) viewModel.retry() }
    }

    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    val savedSearches by viewModel.savedSearches.collectAsStateWithLifecycle()
    var naming by remember { mutableStateOf<SavedName?>(null) }
    val feedAdded = stringResource(Res.string.feed_added)
    val feedFull = stringResource(Res.string.feed_full)
    naming?.let { current ->
        SavedNameDialog(
            initial = current.name,
            onDismiss = { naming = null },
            onConfirm = { name ->
                naming = null
                val search = current.search
                if (search == null) viewModel.saveSearch(name) else viewModel.renameSaved(search, name)
            },
        )
    }
    val uriHandler = LocalUriHandler.current
    var showFilters by rememberSaveable { mutableStateOf(false) }
    if (showFilters) {
        SourceFiltersSheet(
            filters = state.filters,
            defaults = viewModel.defaultFilters(),
            onApply = {
                showFilters = false
                viewModel.search(query.trim(), it)
            },
            onDismiss = { showFilters = false },
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(sourceDisplayName(SourceId(sourceId), state.sourceName), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
                actions = {
                    if (!isLocal) {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(HqrIcons.More, stringResource(Res.string.action_more)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                // RF-24: what is on screen, kept by name.
                                if (state.listing == Listing.SEARCH && state.canSearch) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.saved_save)) },
                                        onClick = {
                                            menu = false
                                            naming = SavedName(null, state.query)
                                        },
                                    )
                                }
                                // RF-25: this source's latest in the feed.
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.feed_add_latest)) },
                                    onClick = {
                                        menu = false
                                        scope.launch { snackbar.showSnackbar(if (viewModel.addToFeed(null)) feedAdded else feedFull) }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.canSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(Res.string.action_search)) },
                    leadingIcon = { Icon(HqrIcons.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) viewModel.search(query.trim()) }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.listing == Listing.POPULAR,
                    onClick = { viewModel.show(Listing.POPULAR) },
                    label = { Text(stringResource(Res.string.browse_popular)) },
                )
                if (state.hasLatest) {
                    FilterChip(
                        selected = state.listing == Listing.LATEST,
                        onClick = { viewModel.show(Listing.LATEST) },
                        label = { Text(stringResource(Res.string.browse_latest)) },
                    )
                }
                // RF-22: the source's filters, with how many are on.
                if (state.filters.isNotEmpty()) {
                    val active = state.activeFilters
                    val description =
                        if (active > 0) stringResource(Res.string.filter_active, active) else stringResource(Res.string.filter_title)
                    FilterChip(
                        selected = active > 0,
                        onClick = { showFilters = true },
                        label = {
                            Text(
                                if (active >
                                    0
                                ) {
                                    "${stringResource(Res.string.filter_title)} · $active"
                                } else {
                                    stringResource(Res.string.filter_title)
                                },
                            )
                        },
                        leadingIcon = { Icon(HqrIcons.Tune, contentDescription = null) },
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                }
            }
            // RF-24: one tap brings a saved search back; its menu renames, deletes or puts it in the feed.
            if (savedSearches.isNotEmpty()) {
                SavedSearchesRow(
                    searches = savedSearches,
                    onApply = { search ->
                        query = search.query
                        viewModel.applySaved(search)
                    },
                    onRename = { naming = SavedName(it, it.name) },
                    onDelete = viewModel::deleteSaved,
                    onFeed = { search -> scope.launch { snackbar.showSnackbar(if (viewModel.addToFeed(search)) feedAdded else feedFull) } },
                )
            }
            when {
                state.loading -> LoadingState()
                state.error == ErrorKind.UNAVAILABLE && imported != null ->
                    EmptyState(
                        title = stringResource(Res.string.files_empty_title),
                        message = importMessage(importResult) ?: stringResource(Res.string.files_empty_message),
                        action = stringResource(Res.string.files_import),
                        onAction = {
                            scope.launch {
                                // The picker first: the browser opens it only right after the click (RF-110).
                                importResult =
                                    runCatching { imported.pick() }.getOrElse {
                                        ImportResult(
                                            emptyList(),
                                            emptyList(),
                                            failed = listOf("?"),
                                        )
                                    }
                                if (importResult?.imported?.isNotEmpty() == true) runCatching { imported.persist() }
                            }
                        },
                    )
                state.error == ErrorKind.UNAVAILABLE && isLocal ->
                    EmptyState(
                        title = stringResource(Res.string.local_no_folder),
                        message = stringResource(Res.string.local_no_folder_message),
                        action = stringResource(Res.string.action_choose_folder),
                        onAction = pickFolder,
                    )
                // RF-108: in the browser, an online source without a bridge asks for one.
                // RF-106: Cloudflare wants to check the user, in the source's page.
                state.error == ErrorKind.CHALLENGE && state.webUrl != null ->
                    ErrorState(
                        title = stringResource(Res.string.error_challenge),
                        message = stringResource(Res.string.challenge_hint),
                        action = stringResource(Res.string.challenge_verify),
                        onAction = {
                            state.webUrl?.let {
                                verifying = true
                                onChallenge(it)
                            }
                        },
                        secondary = stringResource(Res.string.action_retry),
                        onSecondary = viewModel::retry,
                    )
                state.error == ErrorKind.BRIDGE ->
                    EmptyState(
                        title = stringResource(Res.string.bridge_needed),
                        message = stringResource(Res.string.error_bridge),
                        action = stringResource(Res.string.bridge_configure),
                        onAction = onBridge,
                    )
                state.error != null && state.items.isEmpty() ->
                    ErrorState(
                        title = stringResource(Res.string.error_title),
                        message = stringResource(state.error!!.message()),
                        action = stringResource(Res.string.action_retry),
                        onAction = viewModel::retry,
                        // RF-05: when the source has a web page.
                        secondary = state.webUrl?.let { stringResource(Res.string.action_open_browser) },
                        onSecondary = { state.webUrl?.let(uriHandler::openUri) },
                    )
                state.isEmpty ->
                    EmptyState(stringResource(Res.string.browse_no_results), stringResource(Res.string.browse_no_results_message))
                else ->
                    // RF-15: pulling reads the source again (a local folder shows new and removed files).
                    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = viewModel::refresh) {
                        LazyVerticalGrid(
                            state = grid,
                            columns = GridCells.Adaptive(108.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.items, key = { it.key }) { manga ->
                                Column(
                                    // RF-52: a long press puts the work in the library or takes it out, with "Desfazer".
                                    Modifier.combinedClickable(
                                        onClick = { scope.launch { onOpenManga(viewModel.open(manga)) } },
                                        onLongClick = {
                                            scope.launch {
                                                val removal = viewModel.toggleLibrary(manga)
                                                val result =
                                                    snackbar.showSnackbar(
                                                        getString(
                                                            if (removal ==
                                                                null
                                                            ) {
                                                                Res.string.browse_added
                                                            } else {
                                                                Res.string.library_removed
                                                            },
                                                        ),
                                                        getString(Res.string.action_undo),
                                                        duration = SnackbarDuration.Long,
                                                    )
                                                if (result == SnackbarResult.ActionPerformed) viewModel.undoToggle(manga, removal)
                                            }
                                        },
                                        onLongClickLabel =
                                            stringResource(
                                                if (manga.key in
                                                    state.inLibrary
                                                ) {
                                                    Res.string.manga_remove_library
                                                } else {
                                                    Res.string.manga_add_library
                                                },
                                            ),
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Cover(manga) {
                                        if (manga.key in state.inLibrary) InLibraryBadge(stringResource(Res.string.browse_in_library))
                                    }
                                    Text(
                                        manga.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            // RF-21: a failed next page keeps the list and asks to try again at the end.
                            state.loadMoreError?.let { kind ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    InlineError(
                                        message = stringResource(kind.message()),
                                        action = stringResource(Res.string.action_retry),
                                        onAction = viewModel::loadMore,
                                    )
                                }
                            }
                            if (state.loadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularProgressIndicator() }
                                }
                            }
                        }
                    }
            }
        }
    }
}

/** The saved search being named: null for a new one. */
private data class SavedName(
    val search: SavedSearch?,
    val name: String,
)

@Composable
private fun SavedNameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.saved_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.saved_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun SavedSearchesRow(
    searches: List<SavedSearch>,
    onApply: (SavedSearch) -> Unit,
    onRename: (SavedSearch) -> Unit,
    onDelete: (SavedSearch) -> Unit,
    onFeed: (SavedSearch) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(searches, key = { it.id }) { search ->
            var open by remember { mutableStateOf(false) }
            Box {
                InputChip(
                    selected = false,
                    onClick = { onApply(search) },
                    label = { Text(search.name) },
                    leadingIcon = { Icon(HqrIcons.Bookmark, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        Icon(
                            HqrIcons.More,
                            stringResource(Res.string.saved_options, search.name),
                            Modifier.minimumInteractiveComponentSize().clickable { open = true }.size(18.dp),
                        )
                    },
                )
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.feed_add)) }, onClick = {
                        open = false
                        onFeed(search)
                    })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.saved_rename)) }, onClick = {
                        open = false
                        onRename(search)
                    })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.saved_delete)) }, onClick = {
                        open = false
                        onDelete(search)
                    })
                }
            }
        }
    }
}
