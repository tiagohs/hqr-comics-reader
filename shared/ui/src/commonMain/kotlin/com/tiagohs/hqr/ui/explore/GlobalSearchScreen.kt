package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel.GroupState
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.InLibraryBadge
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.components.Skeleton
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.components.sharedCover
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.components.sourceMessage
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_open_browser
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.browse_added
import com.tiagohs.hqr.ui.resources.browse_in_library
import com.tiagohs.hqr.ui.resources.browse_no_results_message
import com.tiagohs.hqr.ui.resources.library_removed
import com.tiagohs.hqr.ui.resources.manga_add_library
import com.tiagohs.hqr.ui.resources.manga_remove_library
import com.tiagohs.hqr.ui.resources.search_group_empty
import com.tiagohs.hqr.ui.resources.search_hint
import com.tiagohs.hqr.ui.resources.search_more
import com.tiagohs.hqr.ui.resources.search_nothing_for
import com.tiagohs.hqr.ui.resources.search_recent
import com.tiagohs.hqr.ui.resources.search_recent_remove
import com.tiagohs.hqr.ui.resources.search_see_sources
import com.tiagohs.hqr.ui.theme.HqrIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-SRCH-001: one query, every source, one row per source with its own state (RF-23, RF-26, RF-28). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onOpenSource: (sourceId: String, query: String) -> Unit,
    onSources: () -> Unit = onBack,
    viewModel: GlobalSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) { if (state.query.isEmpty()) focus.requestFocus() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::type,
                        placeholder = { Text(stringResource(Res.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val recent by viewModel.recent.collectAsStateWithLifecycle()
            if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.nothingFound) {
                EmptyState(
                    title = stringResource(Res.string.search_nothing_for, state.searched),
                    message = stringResource(Res.string.browse_no_results_message),
                    action = stringResource(Res.string.search_see_sources),
                    onAction = onSources,
                )
                return@Column
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                // RF-27: with the field empty, the last searches, each with "remover".
                if (state.query.isBlank() && recent.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.search_recent),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(recent, key = { "recent:$it" }) { text ->
                        ListItem(
                            headlineContent = { Text(text) },
                            leadingContent = { Icon(HqrIcons.History, null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeRecent(text) }) {
                                    Icon(HqrIcons.Close, stringResource(Res.string.search_recent_remove, text))
                                }
                            },
                            modifier = Modifier.clickable { viewModel.searchAgain(text) },
                        )
                    }
                }
                items(state.groups, key = { it.sourceId.value }) { group ->
                    SourceGroup(
                        group = group,
                        onRetry = { viewModel.retry(group.sourceId) },
                        onMore = { onOpenSource(group.sourceId.value, state.searched) },
                        onOpen = { manga -> scope.launch { onOpenManga(viewModel.open(manga)) } },
                        onBrowser = viewModel.webUrl(group.sourceId)?.let { url -> { uriHandler.openUri(url) } },
                        onLongPress = { manga ->
                            // RF-52: as in the source screen, with "Desfazer".
                            scope.launch {
                                val removal = viewModel.toggleLibrary(manga)
                                val result =
                                    snackbar.showSnackbar(
                                        getString(if (removal == null) Res.string.browse_added else Res.string.library_removed),
                                        getString(Res.string.action_undo),
                                        duration = SnackbarDuration.Long,
                                    )
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoToggle(manga, removal)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceGroup(
    group: GlobalSearchViewModel.Group,
    onRetry: () -> Unit,
    onMore: () -> Unit,
    onOpen: (SourceManga) -> Unit,
    onBrowser: (() -> Unit)?,
    onLongPress: (SourceManga) -> Unit,
) {
    val name = sourceDisplayName(group.sourceId, group.sourceName)
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (group.state is GroupState.Results && !group.isEmpty) {
                TextButton(onClick = onMore) { Text(stringResource(Res.string.search_more, name)) }
            }
        }
        when (val s = group.state) {
            // Skeletons with the final shape (UX §8); a slow source never holds the other rows.
            GroupState.Loading ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(SKELETONS) { Skeleton(Modifier.width(COVER_WIDTH.dp).aspectRatio(COVER_RATIO)) }
                }
            is GroupState.Error ->
                InlineError(
                    message = s.kind.sourceMessage(name),
                    action = stringResource(Res.string.action_retry),
                    onAction = onRetry,
                    secondary = onBrowser?.let { stringResource(Res.string.action_open_browser) },
                    onSecondary = { onBrowser?.invoke() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            is GroupState.Results ->
                if (s.items.isEmpty()) {
                    Text(
                        stringResource(Res.string.search_group_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.items, key = { it.key }) { manga ->
                            val inLibrary = manga.key in group.inLibrary
                            Column(
                                Modifier
                                    .width(COVER_WIDTH.dp)
                                    .combinedClickable(
                                        onClick = { onOpen(manga) },
                                        onLongClick = { onLongPress(manga) },
                                        onLongClickLabel =
                                            stringResource(
                                                if (inLibrary) Res.string.manga_remove_library else Res.string.manga_add_library,
                                            ),
                                    ),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Cover(manga, Modifier.sharedCover(manga)) {
                                    if (manga.key in group.inLibrary) InLibraryBadge(stringResource(Res.string.browse_in_library))
                                }
                                Text(
                                    manga.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
        }
    }
}

private const val COVER_WIDTH = 108
private const val COVER_RATIO = 2f / 3f
private const val SKELETONS = 3
