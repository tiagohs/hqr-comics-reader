package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.explore.SourceMonogram
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.server_delete
import com.tiagohs.hqr.ui.resources.server_edit_title
import com.tiagohs.hqr.ui.resources.server_open
import com.tiagohs.hqr.ui.resources.settings_local_folder
import com.tiagohs.hqr.ui.resources.settings_no_folder
import com.tiagohs.hqr.ui.resources.sources_add_server
import com.tiagohs.hqr.ui.resources.sources_local
import com.tiagohs.hqr.ui.resources.sources_off_by_list
import com.tiagohs.hqr.ui.resources.sources_online
import com.tiagohs.hqr.ui.resources.sources_servers
import com.tiagohs.hqr.ui.resources.sources_servers_empty
import com.tiagohs.hqr.ui.resources.sources_title
import com.tiagohs.hqr.ui.settings.rememberFolderPicker
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** SCR-SRC-001: online sources with their switches (RF-03, RF-105), the local folder (RF-13) and the servers (RF-17). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (String) -> Unit = {},
    onOpenServer: (String) -> Unit = {},
    viewModel: SourcesViewModel = koinViewModel(),
    prefs: AppPreferences = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val folder by prefs.localFolder.collectAsStateWithLifecycle()
    val pick = rememberFolderPicker { }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.sources_title), style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item { Section(Res.string.sources_online) }
            items(state.online, key = { it.id.value }) { row ->
                val offByList = row.status == SourceRegistry.Status.OFF_BY_LIST
                ListItem(
                    headlineContent = { Text(sourceDisplayName(row.id, row.name)) },
                    supportingContent = {
                        Text(
                            if (offByList) {
                                stringResource(Res.string.sources_off_by_list)
                            } else {
                                row.languages.joinToString(" · ") { it.uppercase() }
                            },
                        )
                    },
                    leadingContent = { SourceMonogram(row.name) },
                    trailingContent = { Switch(checked = row.on, onCheckedChange = null, enabled = !offByList) },
                    modifier =
                        Modifier.toggleable(value = row.on, enabled = !offByList, role = Role.Switch) {
                            viewModel.setEnabled(row.id, it)
                        },
                )
            }
            item { Section(Res.string.sources_local) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_local_folder)) },
                    supportingContent = {
                        Text(folder?.substringAfterLast("%3A")?.replace("%2F", "/") ?: stringResource(Res.string.settings_no_folder))
                    },
                    leadingContent = { Icon(HqrIcons.Folder, null) },
                    modifier = Modifier.clickable { pick() },
                )
            }
            item { Section(Res.string.sources_servers) }
            items(state.servers, key = { it.id.value }) { server ->
                ListItem(
                    headlineContent = { Text(server.name) },
                    supportingContent = { Text(server.url, maxLines = 1) },
                    leadingContent = { Icon(HqrIcons.Server, null) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEditServer(server.id.value) }) {
                                Icon(HqrIcons.EditNote, stringResource(Res.string.server_edit_title) + " " + server.name)
                            }
                            IconButton(onClick = { viewModel.deleteServer(server.id) }) {
                                Icon(HqrIcons.Delete, stringResource(Res.string.server_delete, server.name))
                            }
                        }
                    },
                    modifier =
                        Modifier.clickable(
                            onClickLabel = stringResource(Res.string.server_open, server.name),
                        ) { onOpenServer(server.id.value) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.sources_add_server)) },
                    supportingContent = { Text(stringResource(Res.string.sources_servers_empty)) },
                    leadingContent = { Icon(HqrIcons.Add, null) },
                    modifier = Modifier.clickable(onClick = onAddServer),
                )
            }
        }
    }
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
