package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.repository.RestoreProblem
import com.tiagohs.hqr.presentation.settings.BackupViewModel
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.backup_create
import com.tiagohs.hqr.ui.resources.backup_create_hint
import com.tiagohs.hqr.ui.resources.backup_newer
import com.tiagohs.hqr.ui.resources.backup_not_a_backup
import com.tiagohs.hqr.ui.resources.backup_not_saved
import com.tiagohs.hqr.ui.resources.backup_restore
import com.tiagohs.hqr.ui.resources.backup_restore_hint
import com.tiagohs.hqr.ui.resources.backup_restored
import com.tiagohs.hqr.ui.resources.backup_restored_servers
import com.tiagohs.hqr.ui.resources.backup_restored_title
import com.tiagohs.hqr.ui.resources.backup_saved
import com.tiagohs.hqr.ui.resources.backup_title
import com.tiagohs.hqr.ui.resources.backup_unavailable
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-SET-002: create a backup file and restore one, with the summary of what came in (RF-72, RF-73, RF-89). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(onSaved = viewModel::saved, onOpened = viewModel::restore)
    // The file is ready: the system picker asks where it goes.
    LaunchedEffect(state.pending) { state.pending?.let { files.save(it, viewModel.fileName) } }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.backup_title), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ListItem(
                headlineContent = { Text(stringResource(Res.string.backup_create)) },
                supportingContent = { Text(stringResource(Res.string.backup_create_hint)) },
                leadingContent = { Icon(HqrIcons.Download, null) },
                modifier = Modifier.clickable(enabled = !state.busy, onClick = viewModel::create),
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.backup_restore)) },
                supportingContent = { Text(stringResource(Res.string.backup_restore_hint)) },
                leadingContent = { Icon(HqrIcons.Refresh, null) },
                modifier = Modifier.clickable(enabled = !state.busy, onClick = files.open),
            )
            state.saved?.let { ok ->
                Text(
                    stringResource(if (ok) Res.string.backup_saved else Res.string.backup_not_saved),
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
    state.summary?.let { s ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(Res.string.backup_restored_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.backup_restored, s.works, s.chaptersRead, s.categories, s.servers))
                    if (s.servers > 0) Text(stringResource(Res.string.backup_restored_servers), modifier = Modifier.padding(top = 8.dp))
                    if (s.unavailableSources.isNotEmpty()) {
                        Text(
                            stringResource(Res.string.backup_unavailable, s.unavailableSources.joinToString()),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(Res.string.action_ok)) } },
        )
    }
    state.problem?.let { problem ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            text = {
                Text(
                    stringResource(
                        if (problem ==
                            RestoreProblem.NEWER_VERSION
                        ) {
                            Res.string.backup_newer
                        } else {
                            Res.string.backup_not_a_backup
                        },
                    ),
                )
            },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(Res.string.action_ok)) } },
        )
    }
}
