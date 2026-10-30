package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.demographic_none
import com.tiagohs.hqr.ui.resources.filter_apply
import com.tiagohs.hqr.ui.resources.filter_demographic
import com.tiagohs.hqr.ui.resources.filter_excluded
import com.tiagohs.hqr.ui.resources.filter_order
import com.tiagohs.hqr.ui.resources.filter_original_language
import com.tiagohs.hqr.ui.resources.filter_reset
import com.tiagohs.hqr.ui.resources.filter_status
import com.tiagohs.hqr.ui.resources.filter_tags
import com.tiagohs.hqr.ui.resources.filter_tags_hint
import com.tiagohs.hqr.ui.resources.filter_title
import com.tiagohs.hqr.ui.resources.filter_without
import com.tiagohs.hqr.ui.resources.filter_year
import com.tiagohs.hqr.ui.resources.language_en
import com.tiagohs.hqr.ui.resources.language_ja
import com.tiagohs.hqr.ui.resources.language_ko
import com.tiagohs.hqr.ui.resources.language_pt_br
import com.tiagohs.hqr.ui.resources.language_zh
import com.tiagohs.hqr.ui.resources.library_sort_ascending
import com.tiagohs.hqr.ui.resources.library_sort_descending
import com.tiagohs.hqr.ui.resources.order_created
import com.tiagohs.hqr.ui.resources.order_followed
import com.tiagohs.hqr.ui.resources.order_latest_chapter
import com.tiagohs.hqr.ui.resources.order_rating
import com.tiagohs.hqr.ui.resources.order_relevance
import com.tiagohs.hqr.ui.resources.order_title
import com.tiagohs.hqr.ui.resources.order_year
import com.tiagohs.hqr.ui.resources.status_cancelled
import com.tiagohs.hqr.ui.resources.status_completed
import com.tiagohs.hqr.ui.resources.status_hiatus
import com.tiagohs.hqr.ui.resources.status_ongoing
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * SCR-EXP-003: the source's filters as chips, with "Buscar" fixed at the bottom (RF-22). Tags cycle off → included →
 * excluded; an excluded tag reads "Sem Romance" with the "−" mark, never struck through (design.md "Filtros").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceFiltersSheet(
    filters: List<SourceFilter>,
    defaults: List<SourceFilter>,
    onApply: (List<SourceFilter>) -> Unit,
    onDismiss: () -> Unit,
) {
    // A draft: nothing changes on the screen until "Buscar".
    var draft by remember(filters) { mutableStateOf(filters) }

    fun change(filter: SourceFilter) {
        draft = draft.map { if (it.key == filter.key) filter else it }
    }
    // Fully open: the long tag list scrolls and "Ver resultados" stays on screen (design.md "Filtros").
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.filter_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { draft = defaults.ifEmpty { filters } }) { Text(stringResource(Res.string.filter_reset)) }
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            draft.forEach { filter ->
                when (filter) {
                    is SourceFilter.Tags -> TagsGroup(filter, ::change)
                    is SourceFilter.Choice -> ChoiceGroup(filter, ::change)
                    is SourceFilter.Text -> TextGroup(filter, ::change)
                    is SourceFilter.Sort -> SortGroup(filter, ::change)
                }
            }
        }
        HorizontalDivider()
        Button(
            onClick = { onApply(draft) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .heightIn(min = 56.dp),
        ) { Text(stringResource(Res.string.filter_apply)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(
    key: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            groupTitle(key),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun TagsGroup(
    filter: SourceFilter.Tags,
    onChange: (SourceFilter) -> Unit,
) {
    val excludedState = stringResource(Res.string.filter_excluded)
    Group(filter.key, stringResource(Res.string.filter_tags_hint)) {
        filter.options.forEach { option ->
            val included = option.value in filter.included
            val excluded = option.value in filter.excluded
            val label = if (excluded) stringResource(Res.string.filter_without, option.label) else option.label
            FilterChip(
                selected = included || excluded,
                onClick = {
                    onChange(
                        when {
                            included -> filter.copy(included = filter.included - option.value, excluded = filter.excluded + option.value)
                            excluded -> filter.copy(excluded = filter.excluded - option.value)
                            else -> filter.copy(included = filter.included + option.value)
                        },
                    )
                },
                label = { Text(label) },
                leadingIcon =
                    when {
                        included -> ({ Icon(HqrIcons.Check, null) })
                        excluded -> ({ Icon(HqrIcons.Minus, null) })
                        else -> null
                    },
                colors =
                    if (excluded) {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        FilterChipDefaults.filterChipColors()
                    },
                // design.md "Filtros": an excluded tag reads as such, like aria-pressed="mixed".
                modifier = Modifier.semantics { if (excluded) stateDescription = excludedState },
            )
        }
    }
}

@Composable
private fun ChoiceGroup(
    filter: SourceFilter.Choice,
    onChange: (SourceFilter) -> Unit,
) {
    Group(filter.key) {
        filter.options.forEach { option ->
            val on = option.value in filter.selected
            FilterChip(
                selected = on,
                onClick = {
                    val next =
                        when {
                            on -> filter.selected - option.value
                            filter.multiple -> filter.selected + option.value
                            else -> setOf(option.value)
                        }
                    onChange(filter.copy(selected = next))
                },
                label = { Text(optionLabel(option)) },
                leadingIcon = if (on) ({ Icon(HqrIcons.Check, null) }) else null,
            )
        }
    }
}

@Composable
private fun TextGroup(
    filter: SourceFilter.Text,
    onChange: (SourceFilter) -> Unit,
) {
    Group(filter.key) {
        OutlinedTextField(
            value = filter.value,
            onValueChange = { text -> onChange(filter.copy(value = text.filter { it.isDigit() }.take(YEAR_DIGITS))) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(groupTitle(filter.key)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortGroup(
    filter: SourceFilter.Sort,
    onChange: (SourceFilter) -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            groupTitle(filter.key),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        filter.options.forEach { option ->
            val on = filter.selected == option.value
            ListItem(
                headlineContent = { Text(optionLabel(option)) },
                leadingContent = { RadioButton(selected = on, onClick = null) },
                modifier = Modifier.selectable(selected = on, role = Role.RadioButton) { onChange(filter.copy(selected = option.value)) },
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf(true to Res.string.library_sort_ascending, false to Res.string.library_sort_descending).forEachIndexed {
                i,
                (up, label),
                ->
                SegmentedButton(
                    selected = filter.ascending == up,
                    onClick = { onChange(filter.copy(ascending = up)) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(stringResource(label)) }
            }
        }
    }
}

private val groupTitles =
    mapOf(
        "tags" to Res.string.filter_tags,
        "status" to Res.string.filter_status,
        "demographic" to Res.string.filter_demographic,
        "originalLanguage" to Res.string.filter_original_language,
        "year" to Res.string.filter_year,
        "order" to Res.string.filter_order,
    )

private val optionLabels: Map<String, StringResource> =
    mapOf(
        "ongoing" to Res.string.status_ongoing,
        "completed" to Res.string.status_completed,
        "hiatus" to Res.string.status_hiatus,
        "cancelled" to Res.string.status_cancelled,
        "none" to Res.string.demographic_none,
        "ja" to Res.string.language_ja,
        "ko" to Res.string.language_ko,
        "zh" to Res.string.language_zh,
        "en" to Res.string.language_en,
        "pt-br" to Res.string.language_pt_br,
        "relevance" to Res.string.order_relevance,
        "latestUploadedChapter" to Res.string.order_latest_chapter,
        "followedCount" to Res.string.order_followed,
        "createdAt" to Res.string.order_created,
        "title" to Res.string.order_title,
        "year" to Res.string.order_year,
        "rating" to Res.string.order_rating,
    )

/** Known keys are named in the app language; a source's own words (tags, demographics) stay as given. */
@Composable
private fun groupTitle(key: String): String = groupTitles[key]?.let { stringResource(it) } ?: key

@Composable
private fun optionLabel(option: SourceFilter.Option): String =
    optionLabels[option.value]?.let { stringResource(it) } ?: option.label.replaceFirstChar { it.uppercase() }

private const val YEAR_DIGITS = 4
