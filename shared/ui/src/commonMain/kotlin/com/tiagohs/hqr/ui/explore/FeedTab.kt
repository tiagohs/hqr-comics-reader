package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.FeedItem
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.feed.FeedViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.feed_edit
import com.tiagohs.hqr.ui.resources.feed_empty_message
import com.tiagohs.hqr.ui.resources.feed_empty_title
import com.tiagohs.hqr.ui.resources.feed_failed
import com.tiagohs.hqr.ui.resources.feed_latest_of
import com.tiagohs.hqr.ui.resources.feed_remove
import com.tiagohs.hqr.ui.theme.HqrIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** SCR-EXP-001's feed (RF-25): the newest works of up to 5 items, each with where it came from; pull to update. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTab(
    onOpenManga: (Long) -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }
    if (editing) FeedEditor(state.items, viewModel, onDismiss = { editing = false })
    PullToRefreshBox(
        isRefreshing = state.loading && state.entries.isNotEmpty(),
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.loading && state.entries.isEmpty() && state.items.isNotEmpty() -> LoadingState()
            state.items.isEmpty() && !state.loading ->
                EmptyState(
                    stringResource(Res.string.feed_empty_title),
                    stringResource(Res.string.feed_empty_message),
                    action = stringResource(Res.string.feed_edit),
                    onAction = { editing = true },
                )
            else ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { editing = true }) { Text(stringResource(Res.string.feed_edit)) }
                            // RF-05: one item that failed doesn't hide the others.
                            state.failed.forEach { (origin, kind) ->
                                Text(
                                    stringResource(Res.string.feed_failed, origin) + " · " + stringResource(kind.message()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    items(state.entries, key = { "${it.manga.sourceId.value}/${it.manga.key}" }) { entry ->
                        Column(
                            Modifier.clickable { scope.launch { onOpenManga(viewModel.open(entry.manga)) } },
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Cover(entry.manga) {}
                            Text(
                                entry.manga.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                entry.origin,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
        }
    }
}

/** SCR-EXP-004: what is in the feed (remove) and what can go in (a source's latest, a saved search). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedEditor(
    items: List<FeedItem>,
    viewModel: FeedViewModel,
    onDismiss: () -> Unit,
    registry: SourceRegistry = koinInject(),
) {
    val active by registry.active.collectAsStateWithLifecycle()
    val saved by remember(viewModel) { viewModel.allSaved }.collectAsStateWithLifecycle(emptyList())
    val full = items.size >= SearchRepository.MAX_FEED

    fun inFeed(
        sourceId: SourceId,
        savedId: Long?,
    ) = items.any { it.sourceId == sourceId && it.savedSearch?.id == savedId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            item {
                Text(
                    stringResource(Res.string.feed_edit) + " (${items.size}/${SearchRepository.MAX_FEED})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(items, key = { "in${it.id}" }) { item ->
                val name =
                    item.savedSearch?.name
                        ?: stringResource(Res.string.feed_latest_of, registry[item.sourceId]?.name ?: item.sourceId.value)
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = {
                        IconButton(
                            onClick = { viewModel.remove(item) },
                        ) { Icon(HqrIcons.Close, stringResource(Res.string.feed_remove, name)) }
                    },
                )
            }
            val options =
                active.filterIsInstance<CatalogSource>().map { it.id to null as Long? } + saved.map { it.sourceId to it.id }
            items(
                options.filter { (source, id) ->
                    !inFeed(source, id)
                },
                key = { (source, id) -> "add${source.value}/$id" },
            ) { (source, id) ->
                val name =
                    saved.firstOrNull { it.id == id }?.name
                        ?: stringResource(Res.string.feed_latest_of, registry[source]?.name ?: source.value)
                ListItem(
                    headlineContent = { Text(name) },
                    leadingContent = { Icon(HqrIcons.Add, null) },
                    modifier = if (full) Modifier else Modifier.clickable { viewModel.add(source, id) },
                )
            }
        }
    }
}
