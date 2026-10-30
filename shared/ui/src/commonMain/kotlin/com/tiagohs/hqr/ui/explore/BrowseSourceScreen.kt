package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel.Listing
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.ErrorState
import com.tiagohs.hqr.ui.components.InLibraryBadge
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_choose_folder
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.action_search
import com.tiagohs.hqr.ui.resources.browse_in_library
import com.tiagohs.hqr.ui.resources.browse_latest
import com.tiagohs.hqr.ui.resources.browse_no_results
import com.tiagohs.hqr.ui.resources.browse_no_results_message
import com.tiagohs.hqr.ui.resources.browse_popular
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.local_no_folder
import com.tiagohs.hqr.ui.resources.local_no_folder_message
import com.tiagohs.hqr.ui.settings.rememberFolderPicker
import com.tiagohs.hqr.ui.theme.HqrIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** SCR-EXP-002: Populares, Recentes and search of one source, with continuous loading (RF-21, RF-22). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSourceScreen(
    sourceId: String,
    onBack: () -> Unit,
    onOpenManga: (Long) -> Unit,
    viewModel: BrowseSourceViewModel = koinViewModel(key = "browse-$sourceId") { parametersOf(SourceId(sourceId)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sourceDisplayName(SourceId(sourceId), state.sourceName), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
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
            }
            when {
                state.loading -> LoadingState()
                state.error == ErrorKind.UNAVAILABLE && isLocal ->
                    EmptyState(
                        title = stringResource(Res.string.local_no_folder),
                        message = stringResource(Res.string.local_no_folder_message),
                        action = stringResource(Res.string.action_choose_folder),
                        onAction = pickFolder,
                    )
                state.error != null && state.items.isEmpty() ->
                    ErrorState(
                        title = stringResource(Res.string.error_title),
                        message = stringResource(state.error!!.message()),
                        action = stringResource(Res.string.action_retry),
                        onAction = viewModel::retry,
                    )
                state.isEmpty ->
                    EmptyState(stringResource(Res.string.browse_no_results), stringResource(Res.string.browse_no_results_message))
                else ->
                    LazyVerticalGrid(
                        state = grid,
                        columns = GridCells.Adaptive(108.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.items, key = { it.key }) { manga ->
                            Column(
                                Modifier.clickable { scope.launch { onOpenManga(viewModel.open(manga)) } },
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
