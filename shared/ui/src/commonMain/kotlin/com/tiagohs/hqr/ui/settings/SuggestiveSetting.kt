package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.settings_adult_confirm
import com.tiagohs.hqr.ui.resources.settings_adult_message
import com.tiagohs.hqr.ui.resources.settings_adult_title
import com.tiagohs.hqr.ui.resources.settings_mangadex_suggestive_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** RF-08: MangaDex's suggestive content, turned on only after the 18+ confirmation (Settings and SCR-SRC-002). */
@Composable
fun SuggestiveSetting(
    prefs: AppPreferences,
    title: StringResource,
) {
    val suggestive by prefs.mangaDexSuggestive.collectAsStateWithLifecycle()
    var askAge by remember { mutableStateOf(false) }
    if (askAge) {
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
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(Res.string.settings_mangadex_suggestive_hint)) },
        trailingContent = { Switch(checked = suggestive, onCheckedChange = null) },
        modifier =
            Modifier.toggleable(value = suggestive, role = Role.Switch) {
                if (suggestive) prefs.setMangaDexSuggestive(false) else askAge = true
            },
    )
}
