package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.source.BridgeCheck
import com.tiagohs.hqr.presentation.sources.BridgeViewModel
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.bridge_address
import com.tiagohs.hqr.ui.resources.bridge_address_hint
import com.tiagohs.hqr.ui.resources.bridge_how
import com.tiagohs.hqr.ui.resources.bridge_intro
import com.tiagohs.hqr.ui.resources.bridge_invalid_address
import com.tiagohs.hqr.ui.resources.bridge_ok
import com.tiagohs.hqr.ui.resources.bridge_outdated
import com.tiagohs.hqr.ui.resources.bridge_remove
import com.tiagohs.hqr.ui.resources.bridge_test
import com.tiagohs.hqr.ui.resources.bridge_title
import com.tiagohs.hqr.ui.resources.bridge_token
import com.tiagohs.hqr.ui.resources.bridge_token_refused
import com.tiagohs.hqr.ui.resources.bridge_unreachable
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val GUIDE = "https://github.com/tiagohs/hqr-comics-reader/tree/rewrite/v1/bridge-worker#readme"

/** SCR-WEB-001: the user's bridge for the online sources in the browser (RF-108, RF-109). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeScreen(
    onBack: () -> Unit,
    viewModel: BridgeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.bridge_title), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.bridge_intro), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { uri.openUri(GUIDE) }) { Text(stringResource(Res.string.bridge_how)) }
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::setAddress,
                    label = { Text(stringResource(Res.string.bridge_address)) },
                    placeholder = { Text(stringResource(Res.string.bridge_address_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.token,
                    onValueChange = viewModel::setToken,
                    label = { Text(stringResource(Res.string.bridge_token)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.check?.let { check ->
                    Text(
                        stringResource(
                            when (check) {
                                BridgeCheck.OK -> Res.string.bridge_ok
                                BridgeCheck.OUTDATED -> Res.string.bridge_outdated
                                BridgeCheck.INVALID_ADDRESS -> Res.string.bridge_invalid_address
                                BridgeCheck.TOKEN_REFUSED -> Res.string.bridge_token_refused
                                BridgeCheck.UNREACHABLE -> Res.string.bridge_unreachable
                            },
                        ),
                        color = if (check == BridgeCheck.OK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = viewModel::test,
                        enabled = !state.testing && state.address.isNotBlank() && state.token.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(Res.string.bridge_test)) }
                    if (state.testing) CircularProgressIndicator()
                    if (state.saved != null) TextButton(onClick = viewModel::remove) { Text(stringResource(Res.string.bridge_remove)) }
                }
            }
        }
    }
}
