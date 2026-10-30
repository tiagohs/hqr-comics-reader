package com.tiagohs.hqr.ui.downloads

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.presentation.downloads.DownloadsViewModel
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_move_down
import com.tiagohs.hqr.ui.resources.action_move_up
import com.tiagohs.hqr.ui.resources.download_cancel
import com.tiagohs.hqr.ui.resources.download_delete
import com.tiagohs.hqr.ui.resources.download_state_error
import com.tiagohs.hqr.ui.resources.download_state_paused
import com.tiagohs.hqr.ui.resources.download_state_queued
import com.tiagohs.hqr.ui.resources.download_state_running
import com.tiagohs.hqr.ui.resources.download_state_wifi
import com.tiagohs.hqr.ui.resources.downloads_delete_after_read
import com.tiagohs.hqr.ui.resources.downloads_delete_after_read_hint
import com.tiagohs.hqr.ui.resources.downloads_delete_all
import com.tiagohs.hqr.ui.resources.downloads_delete_all_confirm
import com.tiagohs.hqr.ui.resources.downloads_delete_work
import com.tiagohs.hqr.ui.resources.downloads_done
import com.tiagohs.hqr.ui.resources.downloads_empty_message
import com.tiagohs.hqr.ui.resources.downloads_empty_title
import com.tiagohs.hqr.ui.resources.downloads_pause_all
import com.tiagohs.hqr.ui.resources.downloads_queue
import com.tiagohs.hqr.ui.resources.downloads_resume_all
import com.tiagohs.hqr.ui.resources.downloads_see_library
import com.tiagohs.hqr.ui.resources.downloads_title
import com.tiagohs.hqr.ui.resources.downloads_used
import com.tiagohs.hqr.ui.resources.downloads_wifi_only
import com.tiagohs.hqr.ui.resources.downloads_wifi_only_hint
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-DL-001: the queue (pause, resume, order, cancel), the downloaded chapters and the space they use (RF-62 to RF-66). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onLibrary: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.downloads_title), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
            actions = {
                if (state.queue.isNotEmpty()) {
                    if (state.paused) {
                        IconButton(onClick = viewModel::resumeAll) { Icon(HqrIcons.Play, stringResource(Res.string.downloads_resume_all)) }
                    } else {
                        IconButton(onClick = viewModel::pauseAll) { Icon(HqrIcons.Pause, stringResource(Res.string.downloads_pause_all)) }
                    }
                }
                if (state.done.isNotEmpty()) {
                    IconButton(onClick = { confirmAll = true }) { Icon(HqrIcons.Delete, stringResource(Res.string.downloads_delete_all)) }
                }
            },
        )
    }) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            else ->
                LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                    item { Options(state, viewModel) }
                    if (state.isEmpty) {
                        item {
                            EmptyState(
                                stringResource(Res.string.downloads_empty_title),
                                stringResource(Res.string.downloads_empty_message),
                                action = stringResource(Res.string.downloads_see_library),
                                onAction = onLibrary,
                            )
                        }
                    }
                    if (state.queue.isNotEmpty()) item { Section(Res.string.downloads_queue) }
                    items(state.queue, key = { "q${it.chapterId}" }) { item ->
                        QueueRow(item, first = item == state.queue.first(), last = item == state.queue.last(), viewModel)
                    }
                    if (state.done.isNotEmpty()) {
                        item {
                            Row(Modifier.fillMaxWidth()) {
                                Section(Res.string.downloads_done)
                            }
                            Text(
                                stringResource(Res.string.downloads_used, formatBytes(state.usedBytes)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    // RF-66: grouped by work, each deletable as a whole or by chapter.
                    state.done.groupBy { it.mangaId }.forEach { (mangaId, chapters) ->
                        item(key = "w$mangaId") {
                            ListItem(
                                headlineContent = { Text(chapters.first().mangaTitle, style = MaterialTheme.typography.titleMedium) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.removeWork(mangaId) }) {
                                        Icon(HqrIcons.Delete, stringResource(Res.string.downloads_delete_work, chapters.first().mangaTitle))
                                    }
                                },
                            )
                        }
                        items(chapters, key = { "d${it.chapterId}" }) { item ->
                            ListItem(
                                headlineContent = { Text(item.chapterLabel) },
                                supportingContent = {
                                    Text(
                                        stringResource(Res.string.download_state_running, item.pagesTotal, item.pagesTotal),
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.remove(item.chapterId) }) {
                                        Icon(HqrIcons.Close, stringResource(Res.string.download_delete, item.chapterLabel))
                                    }
                                },
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
        }
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            text = { Text(stringResource(Res.string.downloads_delete_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAll = false
                    viewModel.removeAll()
                }) { Text(stringResource(Res.string.downloads_delete_all)) }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Options(
    state: DownloadsViewModel.State,
    viewModel: DownloadsViewModel,
) {
    ListItem(
        headlineContent = { Text(stringResource(Res.string.downloads_wifi_only)) },
        supportingContent = { Text(stringResource(Res.string.downloads_wifi_only_hint)) },
        trailingContent = { Switch(checked = state.wifiOnly, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = state.wifiOnly, role = Role.Switch, onValueChange = viewModel::setWifiOnly),
    )
    ListItem(
        headlineContent = { Text(stringResource(Res.string.downloads_delete_after_read)) },
        supportingContent = { Text(stringResource(Res.string.downloads_delete_after_read_hint)) },
        trailingContent = { Switch(checked = state.deleteAfterRead, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = state.deleteAfterRead, role = Role.Switch, onValueChange = viewModel::setDeleteAfterRead),
    )
}

@Composable
private fun QueueRow(
    item: DownloadItem,
    first: Boolean,
    last: Boolean,
    viewModel: DownloadsViewModel,
) {
    ListItem(
        headlineContent = { Text("${item.mangaTitle} · ${item.chapterLabel}", maxLines = 1) },
        supportingContent = {
            androidx.compose.foundation.layout.Column {
                Text(
                    stateText(item),
                    color =
                        if (item.state ==
                            DownloadState.ERROR
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                if (item.state == DownloadState.RUNNING && item.pagesTotal > 0) {
                    LinearProgressIndicator(
                        progress = { item.pagesDone.toFloat() / item.pagesTotal },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = { viewModel.move(item.chapterId, -1) }, enabled = !first) {
                    Icon(HqrIcons.ArrowUp, stringResource(Res.string.action_move_up, item.chapterLabel))
                }
                IconButton(onClick = { viewModel.move(item.chapterId, 1) }, enabled = !last) {
                    Icon(HqrIcons.ArrowDown, stringResource(Res.string.action_move_down, item.chapterLabel))
                }
                IconButton(onClick = { viewModel.remove(item.chapterId) }) {
                    Icon(HqrIcons.Close, stringResource(Res.string.download_cancel, item.chapterLabel))
                }
            }
        },
    )
}

@Composable
private fun stateText(item: DownloadItem): String =
    when (item.state) {
        DownloadState.QUEUED -> stringResource(Res.string.download_state_queued)
        DownloadState.RUNNING -> stringResource(Res.string.download_state_running, item.pagesDone, item.pagesTotal)
        DownloadState.PAUSED -> stringResource(Res.string.download_state_paused)
        DownloadState.WAITING_WIFI -> stringResource(Res.string.download_state_wifi)
        DownloadState.ERROR -> stringResource(Res.string.download_state_error, item.error.orEmpty())
        DownloadState.DONE -> ""
    }

@Composable
private fun Section(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

/** "12,4 MB": one decimal, binary units. */
internal fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= KILO && unit < units.lastIndex) {
        value /= KILO
        unit++
    }
    val rounded = (value * TENTH).toLong() / TENTH
    return "$rounded ${units[unit]}"
}

private const val KILO = 1024.0
private const val TENTH = 10.0
