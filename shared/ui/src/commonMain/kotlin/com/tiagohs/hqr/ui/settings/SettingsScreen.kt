package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.ThemeMode
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.Updates
import com.tiagohs.hqr.presentation.settings.CONTENT_LANGUAGES
import com.tiagohs.hqr.ui.di.CrashReports
import com.tiagohs.hqr.ui.downloads.formatBytes
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_ok
import com.tiagohs.hqr.ui.resources.nav_settings
import com.tiagohs.hqr.ui.resources.settings_about
import com.tiagohs.hqr.ui.resources.settings_appearance
import com.tiagohs.hqr.ui.resources.settings_backup
import com.tiagohs.hqr.ui.resources.settings_cache
import com.tiagohs.hqr.ui.resources.settings_cache_clear
import com.tiagohs.hqr.ui.resources.settings_cache_size
import com.tiagohs.hqr.ui.resources.settings_content_languages
import com.tiagohs.hqr.ui.resources.settings_cover_color
import com.tiagohs.hqr.ui.resources.settings_cover_color_hint
import com.tiagohs.hqr.ui.resources.settings_crash_reports
import com.tiagohs.hqr.ui.resources.settings_crash_reports_hint
import com.tiagohs.hqr.ui.resources.settings_downloads
import com.tiagohs.hqr.ui.resources.settings_dynamic_color
import com.tiagohs.hqr.ui.resources.settings_dynamic_color_hint
import com.tiagohs.hqr.ui.resources.settings_language
import com.tiagohs.hqr.ui.resources.settings_language_system
import com.tiagohs.hqr.ui.resources.settings_local_folder
import com.tiagohs.hqr.ui.resources.settings_manage_sources
import com.tiagohs.hqr.ui.resources.settings_mangadex_suggestive
import com.tiagohs.hqr.ui.resources.settings_no_folder
import com.tiagohs.hqr.ui.resources.settings_other
import com.tiagohs.hqr.ui.resources.settings_pure_black
import com.tiagohs.hqr.ui.resources.settings_pure_black_hint
import com.tiagohs.hqr.ui.resources.settings_sources
import com.tiagohs.hqr.ui.resources.settings_theme_dark
import com.tiagohs.hqr.ui.resources.settings_theme_light
import com.tiagohs.hqr.ui.resources.settings_theme_system
import com.tiagohs.hqr.ui.resources.settings_updates
import com.tiagohs.hqr.ui.resources.web_crash_reason
import com.tiagohs.hqr.ui.resources.web_downloads_reason
import com.tiagohs.hqr.ui.resources.web_only_phone
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.dynamicColorAvailable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/** SCR-SET-001 (the full list arrives with FEAT-020): appearance, cover color and the local folder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSources: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onBackup: () -> Unit = {},
    onAbout: () -> Unit = {},
    onUpdates: () -> Unit = {},
    prefs: AppPreferences = koinInject(),
    cache: ImageCache = koinInject(),
) {
    val hasDownloads = getKoin().getOrNull<Downloads>() != null
    val hasUpdates = getKoin().getOrNull<Updates>() != null
    val crashReports = getKoin().getOrNull<CrashReports>()
    val inBrowser = LocalRunsInBrowser.current
    val sendCrashes by prefs.crashReports.collectAsStateWithLifecycle()
    val pureBlack by prefs.pureBlack.collectAsStateWithLifecycle()
    val dynamicColor by prefs.dynamicColor.collectAsStateWithLifecycle()
    val language by prefs.appLanguage.collectAsStateWithLifecycle()
    var choosingLanguage by remember { mutableStateOf(false) }
    var choosingContent by remember { mutableStateOf(false) }
    val contentLanguages by prefs.contentLanguages.collectAsStateWithLifecycle()
    if (choosingContent) {
        AlertDialog(
            onDismissRequest = { choosingContent = false },
            title = { Text(stringResource(Res.string.settings_content_languages)) },
            text = { ContentLanguageChips(contentLanguages, prefs::setContentLanguages) },
            confirmButton = { TextButton(onClick = { choosingContent = false }) { Text(stringResource(Res.string.action_ok)) } },
        )
    }
    // RF-80: the cache size, read again after clearing.
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { cacheBytes = cache.size() }
    if (choosingLanguage) {
        LanguageDialog(language, onDismiss = { choosingLanguage = false }) {
            choosingLanguage = false
            prefs.setAppLanguage(it)
        }
    }
    val theme by prefs.theme.collectAsStateWithLifecycle()
    val coverColor by prefs.coverColor.collectAsStateWithLifecycle()
    val folder by prefs.localFolder.collectAsStateWithLifecycle()
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
                    trailingContent = { Switch(checked = coverColor, onCheckedChange = null) },
                    modifier = Modifier.toggleable(value = coverColor, role = Role.Switch, onValueChange = prefs::setCoverColor),
                )
            }
            // RF-76: pure black for the dark theme.
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_pure_black)) },
                    supportingContent = { Text(stringResource(Res.string.settings_pure_black_hint)) },
                    trailingContent = { Switch(checked = pureBlack, onCheckedChange = null) },
                    modifier = Modifier.toggleable(value = pureBlack, role = Role.Switch, onValueChange = prefs::setPureBlack),
                )
            }
            // RF-76: the system's colors, only where the device has them (Android 12 or later).
            if (dynamicColorAvailable) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.settings_dynamic_color)) },
                        supportingContent = { Text(stringResource(Res.string.settings_dynamic_color_hint)) },
                        trailingContent = { Switch(checked = dynamicColor, onCheckedChange = null) },
                        modifier = Modifier.toggleable(value = dynamicColor, role = Role.Switch, onValueChange = prefs::setDynamicColor),
                    )
                }
            }
            // RF-77: the app's language, the device's by default.
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_language)) },
                    supportingContent = { Text(languageName(language)) },
                    leadingContent = { Icon(HqrIcons.Globe, null) },
                    modifier = Modifier.clickable { choosingLanguage = true },
                )
            }
            // RF-81: the languages of the works, chosen at the first opening.
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_content_languages)) },
                    supportingContent = {
                        Text(CONTENT_LANGUAGES.filter { l -> l.codes.any { it in contentLanguages } }.joinToString { it.label })
                    },
                    leadingContent = { Icon(HqrIcons.EditNote, null) },
                    modifier = Modifier.clickable { choosingContent = true },
                )
            }
            item { Section(Res.string.settings_sources) }
            // UX §4: Ajustes leads to sources, downloads and backup (UC-007, UC-009, UC-011).
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_manage_sources)) },
                    leadingContent = { Icon(HqrIcons.Server, null) },
                    modifier = Modifier.clickable(onClick = onSources),
                )
            }
            if (hasDownloads) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.settings_downloads)) },
                        leadingContent = { Icon(HqrIcons.Download, null) },
                        modifier = Modifier.clickable(onClick = onDownloads),
                    )
                }
            } else if (inBrowser) {
                // RF-114: shown with the reason instead of disappearing.
                item { Unavailable(Res.string.settings_downloads, Res.string.web_downloads_reason, HqrIcons.Download) }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_backup)) },
                    leadingContent = { Icon(HqrIcons.Refresh, null) },
                    modifier = Modifier.clickable(onClick = onBackup),
                )
            }
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
            item { SuggestiveSetting(prefs, Res.string.settings_mangadex_suggestive) }
            item { Section(Res.string.settings_other) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_cache)) },
                    supportingContent = { Text(stringResource(Res.string.settings_cache_size, formatBytes(cacheBytes ?: 0))) },
                    trailingContent = {
                        TextButton(onClick = {
                            scope.launch {
                                cache.clear()
                                cacheBytes = cache.size()
                            }
                        }) { Text(stringResource(Res.string.settings_cache_clear)) }
                    },
                )
            }
            if (hasUpdates) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.settings_updates)) },
                        leadingContent = { Icon(HqrIcons.Refresh, null) },
                        modifier = Modifier.clickable(onClick = onUpdates),
                    )
                }
            }
            // RNF-10: crash reports, only where the app sends them.
            if (crashReports == null && inBrowser) {
                item { Unavailable(Res.string.settings_crash_reports, Res.string.web_crash_reason, HqrIcons.Info) }
            }
            if (crashReports != null) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.settings_crash_reports)) },
                        supportingContent = { Text(stringResource(Res.string.settings_crash_reports_hint)) },
                        trailingContent = { Switch(checked = sendCrashes, onCheckedChange = null) },
                        modifier =
                            Modifier.toggleable(value = sendCrashes, role = Role.Switch) {
                                prefs.setCrashReports(it)
                                crashReports.setEnabled(it)
                            },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_about)) },
                    leadingContent = { Icon(HqrIcons.Info, null) },
                    modifier = Modifier.clickable(onClick = onAbout),
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

private val LANGUAGES = listOf(null, "pt", "en", "es")

/** Each language in its own name, so anyone finds theirs (RF-77). */
@Composable
private fun languageName(tag: String?): String =
    when (tag) {
        "pt" -> "Português (Brasil)"
        "en" -> "English"
        "es" -> "Español"
        else -> stringResource(Res.string.settings_language_system)
    }

@Composable
private fun LanguageDialog(
    current: String?,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_language)) },
        text = {
            Column {
                LANGUAGES.forEach { tag ->
                    ListItem(
                        headlineContent = { Text(languageName(tag)) },
                        leadingContent = { RadioButton(selected = current == tag, onClick = null) },
                        modifier = Modifier.selectable(selected = current == tag, role = Role.RadioButton) { onPick(tag) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** RF-114: a feature this platform doesn't have, greyed out with why. */
@Composable
private fun Unavailable(
    title: StringResource,
    reason: StringResource,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    ListItem(
        headlineContent = { Text(stringResource(title), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { Text(stringResource(Res.string.web_only_phone) + " · " + stringResource(reason)) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}
