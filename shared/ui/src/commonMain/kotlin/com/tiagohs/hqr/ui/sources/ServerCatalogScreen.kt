package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.presentation.sources.ServerCatalogViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.ErrorState
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.catalog_book
import com.tiagohs.hqr.ui.resources.catalog_empty
import com.tiagohs.hqr.ui.resources.catalog_empty_message
import com.tiagohs.hqr.ui.resources.catalog_folder
import com.tiagohs.hqr.ui.resources.catalog_more
import com.tiagohs.hqr.ui.resources.catalog_search
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrShapes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** SCR-SRV-002: the server's folders and books, its search, and a book opening its work (RF-18). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ServerCatalogScreen(
    sourceId: String,
    onBack: () -> Unit,
    onOpenManga: (Long) -> Unit,
    viewModel: ServerCatalogViewModel = koinViewModel(key = "catalog-$sourceId") { parametersOf(SourceId(sourceId)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(state.query) }
    // Back walks up the folders first; at the root it leaves the screen.
    val back = { if (!viewModel.up()) onBack() }
    // The system back also walks up the folders before leaving.
    BackHandler(enabled = state.canGoUp) { viewModel.up() }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    state.page?.title ?: state.serverName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = { IconButton(onClick = back) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.page?.canSearch == true || state.query.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(Res.string.catalog_search, state.serverName)) },
                    leadingIcon = { Icon(HqrIcons.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when {
                state.loading -> LoadingState()
                state.error != null && state.page?.entries.isNullOrEmpty() ->
                    ErrorState(
                        title = stringResource(Res.string.error_title),
                        message = stringResource(state.error!!.message()),
                        action = stringResource(Res.string.action_retry),
                        onAction = viewModel::retry,
                    )
                state.isEmpty -> EmptyState(stringResource(Res.string.catalog_empty), stringResource(Res.string.catalog_empty_message))
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.page!!.entries, key = { it.title + it.url }) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                supportingContent = entry.subtitle?.takeIf { it.isNotBlank() }?.let { { Text(it, maxLines = 2) } },
                                leadingContent = {
                                    if (entry.coverUrl != null) {
                                        Box(Modifier.width(48.dp)) {
                                            Cover(
                                                SourceManga(SourceId(sourceId), entry.url, entry.title, coverUrl = entry.coverUrl),
                                                shape = HqrShapes.coverSmall,
                                            )
                                        }
                                    } else {
                                        Icon(
                                            if (entry.isBook) HqrIcons.Library else HqrIcons.Folder,
                                            contentDescription =
                                                stringResource(if (entry.isBook) Res.string.catalog_book else Res.string.catalog_folder),
                                        )
                                    }
                                },
                                trailingContent = { if (!entry.isBook) Icon(HqrIcons.Expand, contentDescription = null) },
                                modifier = Modifier.clickable { viewModel.open(entry, onOpenManga) },
                            )
                        }
                        if (state.page?.nextUrl != null) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator()
                                    } else {
                                        TextButton(onClick = viewModel::loadMore) { Text(stringResource(Res.string.catalog_more)) }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}
