package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.presentation.sources.AddServerViewModel
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_save
import com.tiagohs.hqr.ui.resources.server_add_title
import com.tiagohs.hqr.ui.resources.server_edit_title
import com.tiagohs.hqr.ui.resources.server_name
import com.tiagohs.hqr.ui.resources.server_ok
import com.tiagohs.hqr.ui.resources.server_password
import com.tiagohs.hqr.ui.resources.server_problem_certificate
import com.tiagohs.hqr.ui.resources.server_problem_cleartext
import com.tiagohs.hqr.ui.resources.server_problem_credentials_in_address
import com.tiagohs.hqr.ui.resources.server_problem_invalid_address
import com.tiagohs.hqr.ui.resources.server_problem_not_opds
import com.tiagohs.hqr.ui.resources.server_problem_unreachable
import com.tiagohs.hqr.ui.resources.server_problem_wrong_login
import com.tiagohs.hqr.ui.resources.server_saved_secure
import com.tiagohs.hqr.ui.resources.server_test
import com.tiagohs.hqr.ui.resources.server_url
import com.tiagohs.hqr.ui.resources.server_url_hint
import com.tiagohs.hqr.ui.resources.server_username
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** SCR-SRV-001: name, OPDS address and optional login; "Testar" says the cause of a failure (RF-17, RF-19, RF-95). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    editId: String?,
    onDone: () -> Unit,
    viewModel: AddServerViewModel = koinViewModel(key = "server-${editId.orEmpty()}") { parametersOf(editId?.let(::SourceId)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val input = state.input
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (state.isEdit) Res.string.server_edit_title else Res.string.server_add_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            navigationIcon = { IconButton(onClick = onDone) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
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
            OutlinedTextField(
                value = input.name,
                onValueChange = { viewModel.edit(input.copy(name = it)) },
                label = { Text(stringResource(Res.string.server_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = input.url,
                onValueChange = { viewModel.edit(input.copy(url = it)) },
                label = { Text(stringResource(Res.string.server_url)) },
                placeholder = { Text(stringResource(Res.string.server_url_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = input.username,
                onValueChange = { viewModel.edit(input.copy(username = it)) },
                label = { Text(stringResource(Res.string.server_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = input.password,
                onValueChange = { viewModel.edit(input.copy(password = it)) },
                label = { Text(stringResource(Res.string.server_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = { Text(stringResource(Res.string.server_saved_secure)) },
                modifier = Modifier.fillMaxWidth(),
            )
            when (val result = state.result) {
                is AddServerViewModel.Result.Problem -> InlineError(stringResource(result.problem.message()))
                AddServerViewModel.Result.Ok ->
                    Text(
                        stringResource(Res.string.server_ok),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                null -> Unit
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = viewModel::test, enabled = state.canSubmit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.server_test))
                }
                Button(onClick = viewModel::save, enabled = state.canSubmit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.action_save))
                }
                if (state.busy) CircularProgressIndicator(Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun ServerProblem.message(): StringResource =
    when (this) {
        ServerProblem.INVALID_ADDRESS -> Res.string.server_problem_invalid_address
        ServerProblem.CREDENTIALS_IN_ADDRESS -> Res.string.server_problem_credentials_in_address
        ServerProblem.CLEARTEXT_OUTSIDE_HOME -> Res.string.server_problem_cleartext
        ServerProblem.UNREACHABLE -> Res.string.server_problem_unreachable
        ServerProblem.WRONG_LOGIN -> Res.string.server_problem_wrong_login
        ServerProblem.CERTIFICATE -> Res.string.server_problem_certificate
        ServerProblem.NOT_OPDS -> Res.string.server_problem_not_opds
    }
