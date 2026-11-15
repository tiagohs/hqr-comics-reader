package com.tiagohs.hqr.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.presentation.settings.CONTENT_LANGUAGES
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.onboarding_languages_group
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource

/** The chips of the content languages; at least one stays on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentLanguageChips(
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = stringResource(Res.string.onboarding_languages_group)
    FlowRow(
        modifier.semantics { contentDescription = group },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CONTENT_LANGUAGES.forEach { language ->
            val on = language.codes.any { it in selected }
            FilterChip(
                selected = on,
                onClick = {
                    val next = if (on) selected - language.codes else selected + language.codes
                    if (next.isNotEmpty()) onChange(next)
                },
                label = { Text(language.label) },
                leadingIcon = if (on) ({ Icon(HqrIcons.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null,
            )
        }
    }
}
