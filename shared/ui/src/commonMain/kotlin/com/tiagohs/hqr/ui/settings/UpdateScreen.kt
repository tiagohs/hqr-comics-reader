package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.tiagohs.hqr.domain.repository.UpdateProblem
import com.tiagohs.hqr.presentation.settings.UpdateViewModel
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.update_available
import com.tiagohs.hqr.ui.resources.update_check_failed
import com.tiagohs.hqr.ui.resources.update_checking
import com.tiagohs.hqr.ui.resources.update_install
import com.tiagohs.hqr.ui.resources.update_later
import com.tiagohs.hqr.ui.resources.update_latest
import com.tiagohs.hqr.ui.resources.update_problem_mismatch
import com.tiagohs.hqr.ui.resources.update_problem_network
import com.tiagohs.hqr.ui.resources.update_problem_no_checksum
import com.tiagohs.hqr.ui.resources.update_problem_permission
import com.tiagohs.hqr.ui.resources.update_title
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-SET-004: the check the user asked for, the notes of the new version, and its download (RF-82). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (!state.checked) viewModel.check(force = true) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.update_title), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val info = state.info
            when {
                state.checking -> {
                    Text(stringResource(Res.string.update_checking))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                state.checkFailed -> Text(stringResource(Res.string.update_check_failed), color = MaterialTheme.colorScheme.error)
                info == null -> Text(stringResource(Res.string.update_latest))
                else -> UpdateDetails(state, viewModel)
            }
        }
    }
}

@Composable
private fun UpdateDetails(
    state: UpdateViewModel.State,
    viewModel: UpdateViewModel,
) {
    val info = state.info ?: return
    Text(stringResource(Res.string.update_available, info.version), style = MaterialTheme.typography.titleLarge)
    if (info.notes.isNotBlank()) Text(info.notes, style = MaterialTheme.typography.bodyMedium)
    state.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
    state.installError?.let {
        Text(
            stringResource(
                when (it) {
                    UpdateProblem.NO_CHECKSUM -> Res.string.update_problem_no_checksum
                    UpdateProblem.NEEDS_PERMISSION -> Res.string.update_problem_permission
                    UpdateProblem.CHECKSUM_MISMATCH -> Res.string.update_problem_mismatch
                    UpdateProblem.NETWORK -> Res.string.update_problem_network
                },
            ),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Button(onClick = viewModel::install, enabled = state.progress == null, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(Res.string.update_install))
    }
}

/** RF-82: at most once a day on its own; a new version shows its notes with "Atualizar". */
@Composable
fun UpdateNotice(
    onOpen: () -> Unit,
    viewModel: UpdateViewModel = koinViewModel(key = "update-notice"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.check(force = false) }
    val info = state.info ?: return
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(stringResource(Res.string.update_available, info.version)) },
        text = { Text(info.notes, maxLines = NOTES_LINES) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.dismiss()
                onOpen()
            }) { Text(stringResource(Res.string.update_install)) }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(Res.string.update_later)) } },
    )
}

private const val NOTES_LINES = 12
