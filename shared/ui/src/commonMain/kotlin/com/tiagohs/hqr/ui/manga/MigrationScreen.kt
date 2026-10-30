package com.tiagohs.hqr.ui.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.migrate_confirm
import com.tiagohs.hqr.ui.resources.migrate_confirm_message
import com.tiagohs.hqr.ui.resources.migrate_confirm_title
import com.tiagohs.hqr.ui.resources.migrate_delete_downloads
import com.tiagohs.hqr.ui.resources.migrate_no_sources
import com.tiagohs.hqr.ui.resources.migrate_nothing
import com.tiagohs.hqr.ui.resources.migrate_query
import com.tiagohs.hqr.ui.resources.migrate_sources
import com.tiagohs.hqr.ui.resources.migrate_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** SCR-OBR-002: the work's title searched in the chosen sources; a candidate tapped asks before migrating (RF-69). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MigrationScreen(
    mangaId: Long,
    onBack: () -> Unit,
    onMigrated: (newMangaId: Long, readCarried: Int) -> Unit,
    viewModel: MigrationViewModel = koinViewModel(key = "migrate-$mangaId") { parametersOf(mangaId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { state.done?.let { onMigrated(it.mangaId, it.readCarried) } }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.migrate_title, state.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text(stringResource(Res.string.migrate_query)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) { Icon(HqrIcons.Search, stringResource(Res.string.migrate_query)) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            item { Heading(stringResource(Res.string.migrate_sources)) }
            item {
                if (state.sources.isEmpty() && state.title.isNotEmpty()) {
                    Text(stringResource(Res.string.migrate_no_sources), Modifier.padding(horizontal = 16.dp))
                }
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.sources.forEach { option ->
                        FilterChip(
                            selected = option.chosen,
                            onClick = { viewModel.toggleSource(option.id) },
                            label = { Text(option.name) },
                            leadingIcon =
                                if (option.chosen) {
                                    { Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                                } else {
                                    null
                                },
                        )
                    }
                }
            }
            state.sources.filter { it.chosen }.forEach { option ->
                val results = state.results[option.id]
                item(key = option.id.value) {
                    Heading(option.name)
                    when {
                        results == null || results.loading ->
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        results.error != null ->
                            Text(
                                stringResource(results.error!!.message()),
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        results.items.isEmpty() -> Text(stringResource(Res.string.migrate_nothing), Modifier.padding(horizontal = 16.dp))
                        else ->
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(results.items, key = { it.key }) { candidate ->
                                    Candidate(candidate) { viewModel.pick(candidate) }
                                }
                            }
                    }
                }
            }
        }
    }
    state.picked?.let { candidate ->
        var deleteDownloads by remember(candidate) { mutableStateOf(false) }
        val sourceName = state.sources.firstOrNull { it.id == candidate.sourceId }?.name ?: candidate.sourceId.value
        AlertDialog(
            onDismissRequest = { if (!state.migrating) viewModel.pick(null) },
            title = { Text(stringResource(Res.string.migrate_confirm_title, sourceName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.migrate_confirm_message, candidate.title))
                    Row(
                        Modifier.toggleable(value = deleteDownloads, role = Role.Checkbox) { deleteDownloads = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = deleteDownloads, onCheckedChange = null)
                        Text(stringResource(Res.string.migrate_delete_downloads), Modifier.padding(start = 8.dp))
                    }
                    state.error?.let { Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.migrate(deleteDownloads) }, enabled = !state.migrating) {
                    Text(stringResource(Res.string.migrate_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.pick(null) },
                    enabled = !state.migrating,
                ) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun Candidate(
    manga: SourceManga,
    onClick: () -> Unit,
) {
    Column(Modifier.width(112.dp).clickable(onClick = onClick)) {
        Cover(manga)
        Text(manga.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
