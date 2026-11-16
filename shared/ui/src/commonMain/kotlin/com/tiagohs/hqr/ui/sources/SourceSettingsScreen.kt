package com.tiagohs.hqr.ui.sources

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.tiagohs.hqr.domain.model.Shelf
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.library_area_comics
import com.tiagohs.hqr.ui.resources.library_area_manga
import com.tiagohs.hqr.ui.resources.source_settings_content
import com.tiagohs.hqr.ui.resources.source_settings_follow
import com.tiagohs.hqr.ui.resources.source_settings_follow_hint
import com.tiagohs.hqr.ui.resources.source_settings_languages
import com.tiagohs.hqr.ui.resources.source_settings_quality
import com.tiagohs.hqr.ui.resources.source_settings_quality_hint
import com.tiagohs.hqr.ui.resources.source_settings_quality_original
import com.tiagohs.hqr.ui.resources.source_settings_quality_saver
import com.tiagohs.hqr.ui.resources.source_settings_shelf
import com.tiagohs.hqr.ui.resources.source_settings_shelf_hint
import com.tiagohs.hqr.ui.resources.source_settings_shelf_label
import com.tiagohs.hqr.ui.resources.source_settings_suggestive
import com.tiagohs.hqr.ui.settings.ContentLanguageChips
import com.tiagohs.hqr.ui.settings.SuggestiveSetting
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** SCR-SRC-002: MangaDex's own settings — chapter languages, image quality and suggestive content (RF-06, RF-08). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSettingsScreen(
    onBack: () -> Unit,
    sourceId: String = SourceId.MANGADEX.value,
    sourceName: String = MANGADEX,
    prefs: AppPreferences = koinInject(),
) {
    val shelves by prefs.sourceShelves.collectAsStateWithLifecycle()
    val shelf = Shelf.of(null, SourceId(sourceId), shelves)
    val override by prefs.mangaDexLanguagesOverride.collectAsStateWithLifecycle()
    val content by prefs.contentLanguages.collectAsStateWithLifecycle()
    val saver by prefs.mangaDexDataSaver.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(sourceName, style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            // RF-128: where this source's works go in the library.
            item { Section(Res.string.source_settings_shelf) }
            item {
                Text(
                    stringResource(Res.string.source_settings_shelf_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val options = listOf(Shelf.COMICS to Res.string.library_area_comics, Shelf.MANGA to Res.string.library_area_manga)
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = shelf == value,
                            onClick = { prefs.setSourceShelf(sourceId, value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            item {
                Text(
                    stringResource(Res.string.source_settings_shelf_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            // RF-06: only MangaDex has its own chapter settings.
            if (sourceId != SourceId.MANGADEX.value) return@LazyColumn
            item { Section(Res.string.source_settings_languages) }
            item {
                val follow = override == null
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.source_settings_follow)) },
                    supportingContent = { Text(stringResource(Res.string.source_settings_follow_hint)) },
                    trailingContent = { Switch(checked = follow, onCheckedChange = null) },
                    modifier =
                        Modifier.toggleable(value = follow, role = Role.Switch) {
                            prefs.setMangaDexLanguages(if (it) null else content)
                        },
                )
            }
            item {
                ContentLanguageChips(
                    selected = override ?: content,
                    onChange = { prefs.setMangaDexLanguages(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { Section(Res.string.source_settings_quality) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val options =
                        listOf(
                            false to Res.string.source_settings_quality_original,
                            true to Res.string.source_settings_quality_saver,
                        )
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = saver == value,
                            onClick = { prefs.setMangaDexDataSaver(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            item {
                Text(
                    stringResource(Res.string.source_settings_quality_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { Section(Res.string.source_settings_content) }
            item { SuggestiveSetting(prefs, Res.string.source_settings_suggestive) }
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

private const val MANGADEX = "MangaDex"
