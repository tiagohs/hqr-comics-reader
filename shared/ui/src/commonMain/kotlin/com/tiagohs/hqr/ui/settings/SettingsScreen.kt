package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.ThemeMode
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.nav_settings
import com.tiagohs.hqr.ui.resources.settings_adult_confirm
import com.tiagohs.hqr.ui.resources.settings_adult_message
import com.tiagohs.hqr.ui.resources.settings_adult_title
import com.tiagohs.hqr.ui.resources.settings_appearance
import com.tiagohs.hqr.ui.resources.settings_cover_color
import com.tiagohs.hqr.ui.resources.settings_cover_color_hint
import com.tiagohs.hqr.ui.resources.settings_local_folder
import com.tiagohs.hqr.ui.resources.settings_mangadex_suggestive
import com.tiagohs.hqr.ui.resources.settings_mangadex_suggestive_hint
import com.tiagohs.hqr.ui.resources.settings_no_folder
import com.tiagohs.hqr.ui.resources.settings_sources
import com.tiagohs.hqr.ui.resources.settings_theme_dark
import com.tiagohs.hqr.ui.resources.settings_theme_light
import com.tiagohs.hqr.ui.resources.settings_theme_system
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** SCR-SET-001 (the full list arrives with FEAT-020): appearance, cover color and the local folder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: AppPreferences = koinInject()) {
    val theme by prefs.theme.collectAsStateWithLifecycle()
    val coverColor by prefs.coverColor.collectAsStateWithLifecycle()
    val folder by prefs.localFolder.collectAsStateWithLifecycle()
    val suggestive by prefs.mangaDexSuggestive.collectAsStateWithLifecycle()
    var askAge by remember { mutableStateOf(false) }
    if (askAge) {
        // RF-08: suggestive content only after the 18+ confirmation.
        AlertDialog(
            onDismissRequest = { askAge = false },
            title = { Text(stringResource(Res.string.settings_adult_title)) },
            text = { Text(stringResource(Res.string.settings_adult_message)) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.setMangaDexSuggestive(true)
                    askAge = false
                }) { Text(stringResource(Res.string.settings_adult_confirm)) }
            },
            dismissButton = { TextButton(onClick = { askAge = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
    val toggleSuggestive = { if (suggestive) prefs.setMangaDexSuggestive(false) else askAge = true }
    val pick = rememberFolderPicker { }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(Res.string.nav_settings), style = MaterialTheme.typography.headlineLarge) })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item { Section(Res.string.settings_appearance) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val options =
                        listOf(
                            ThemeMode.SYSTEM to Res.string.settings_theme_system,
                            ThemeMode.LIGHT to Res.string.settings_theme_light,
                            ThemeMode.DARK to Res.string.settings_theme_dark,
                        )
                    options.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = theme == mode,
                            onClick = { prefs.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) {
                            Text(stringResource(label))
                        }
                    }
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_cover_color)) },
                    supportingContent = { Text(stringResource(Res.string.settings_cover_color_hint)) },
                    trailingContent = { Switch(checked = coverColor, onCheckedChange = prefs::setCoverColor) },
                    modifier = Modifier.clickable { prefs.setCoverColor(!coverColor) },
                )
            }
            item { Section(Res.string.settings_sources) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_local_folder)) },
                    supportingContent = {
                        Text(
                            folder?.substringAfterLast("%3A")?.replace("%2F", "/") ?: stringResource(Res.string.settings_no_folder),
                        )
                    },
                    leadingContent = { Icon(HqrIcons.Folder, null) },
                    modifier = Modifier.clickable { pick() },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_mangadex_suggestive)) },
                    supportingContent = { Text(stringResource(Res.string.settings_mangadex_suggestive_hint)) },
                    trailingContent = { Switch(checked = suggestive, onCheckedChange = { toggleSuggestive() }) },
                    modifier = Modifier.clickable { toggleSuggestive() },
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
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
