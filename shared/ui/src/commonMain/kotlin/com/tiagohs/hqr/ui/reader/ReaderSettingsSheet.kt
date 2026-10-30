package com.tiagohs.hqr.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.presentation.reader.PageScale
import com.tiagohs.hqr.presentation.reader.ReaderBackground
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderSettings
import com.tiagohs.hqr.presentation.reader.TapZones
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.reader_background
import com.tiagohs.hqr.ui.resources.reader_bg_auto
import com.tiagohs.hqr.ui.resources.reader_bg_black
import com.tiagohs.hqr.ui.resources.reader_bg_gray
import com.tiagohs.hqr.ui.resources.reader_bg_white
import com.tiagohs.hqr.ui.resources.reader_crop
import com.tiagohs.hqr.ui.resources.reader_crop_hint
import com.tiagohs.hqr.ui.resources.reader_double
import com.tiagohs.hqr.ui.resources.reader_double_hint
import com.tiagohs.hqr.ui.resources.reader_invert_pages
import com.tiagohs.hqr.ui.resources.reader_invert_pages_hint
import com.tiagohs.hqr.ui.resources.reader_keep_on
import com.tiagohs.hqr.ui.resources.reader_keep_on_hint
import com.tiagohs.hqr.ui.resources.reader_mode
import com.tiagohs.hqr.ui.resources.reader_mode_ltr
import com.tiagohs.hqr.ui.resources.reader_mode_rtl
import com.tiagohs.hqr.ui.resources.reader_mode_vertical
import com.tiagohs.hqr.ui.resources.reader_mode_webtoon
import com.tiagohs.hqr.ui.resources.reader_only_this_work
import com.tiagohs.hqr.ui.resources.reader_page_number
import com.tiagohs.hqr.ui.resources.reader_page_number_hint
import com.tiagohs.hqr.ui.resources.reader_scale
import com.tiagohs.hqr.ui.resources.reader_scale_height
import com.tiagohs.hqr.ui.resources.reader_scale_original
import com.tiagohs.hqr.ui.resources.reader_scale_screen
import com.tiagohs.hqr.ui.resources.reader_scale_width
import com.tiagohs.hqr.ui.resources.reader_settings
import com.tiagohs.hqr.ui.resources.reader_split
import com.tiagohs.hqr.ui.resources.reader_split_hint
import com.tiagohs.hqr.ui.resources.reader_volume_keys
import com.tiagohs.hqr.ui.resources.reader_volume_keys_hint
import com.tiagohs.hqr.ui.resources.reader_zones
import com.tiagohs.hqr.ui.resources.reader_zones_default
import com.tiagohs.hqr.ui.resources.reader_zones_edges
import com.tiagohs.hqr.ui.resources.reader_zones_kindle
import com.tiagohs.hqr.ui.resources.reader_zones_off
import com.tiagohs.hqr.ui.resources.reader_zones_sides
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** SCR-RDR-002: mode, tap zones, scale, background and switches; "Só nesta obra" saves per work (RF-37). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onChange: (ReaderSettings, onlyThisWork: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var onlyThisWork by remember { mutableStateOf(true) }
    val update = { s: ReaderSettings -> onChange(s, onlyThisWork) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.reader_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = onlyThisWork,
                    onClick = { onlyThisWork = !onlyThisWork },
                    label = { Text(stringResource(Res.string.reader_only_this_work)) },
                    leadingIcon = if (onlyThisWork) ({ Icon(HqrIcons.Check, null, Modifier.size(18.dp)) }) else null,
                )
            }
            Segmented(
                Res.string.reader_mode,
                listOf(
                    ReaderMode.LEFT_TO_RIGHT to Res.string.reader_mode_ltr,
                    ReaderMode.RIGHT_TO_LEFT to Res.string.reader_mode_rtl,
                    ReaderMode.VERTICAL to Res.string.reader_mode_vertical,
                    ReaderMode.WEBTOON to Res.string.reader_mode_webtoon,
                ),
                settings.mode,
            ) { update(settings.copy(mode = it)) }
            Label(Res.string.reader_zones)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    TapZones.DEFAULT to Res.string.reader_zones_default,
                    TapZones.KINDLE to Res.string.reader_zones_kindle,
                    TapZones.EDGES to Res.string.reader_zones_edges,
                    TapZones.SIDES to Res.string.reader_zones_sides,
                    TapZones.OFF to Res.string.reader_zones_off,
                ).forEach { (zone, label) ->
                    FilterChip(
                        selected = settings.tapZones == zone,
                        onClick = { update(settings.copy(tapZones = zone)) },
                        label = { Text(stringResource(label)) },
                        leadingIcon = if (settings.tapZones == zone) ({ Icon(HqrIcons.Check, null, Modifier.size(18.dp)) }) else null,
                    )
                }
            }
            Segmented(
                Res.string.reader_scale,
                listOf(
                    PageScale.FIT_SCREEN to Res.string.reader_scale_screen,
                    PageScale.FIT_WIDTH to Res.string.reader_scale_width,
                    PageScale.FIT_HEIGHT to Res.string.reader_scale_height,
                    PageScale.ORIGINAL to Res.string.reader_scale_original,
                ),
                settings.scale,
            ) { update(settings.copy(scale = it)) }
            Label(Res.string.reader_background)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(ReaderBackground.BLACK, Color.Black, Res.string.reader_bg_black),
                    Triple(ReaderBackground.GRAY, Color(0xFF3A3A3C), Res.string.reader_bg_gray),
                    Triple(ReaderBackground.WHITE, Color.White, Res.string.reader_bg_white),
                    Triple(ReaderBackground.AUTO, Color(0xFF888888), Res.string.reader_bg_auto),
                ).forEach { (bg, color, label) ->
                    val name = stringResource(label)
                    val selected = settings.background == bg
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(color, CircleShape)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            ).clickable { update(settings.copy(background = bg)) }
                            .semantics {
                                contentDescription = name
                                role = Role.RadioButton
                                this.selected = selected
                            },
                    )
                }
            }
            Toggle(Res.string.reader_double, Res.string.reader_double_hint, settings.doublePage) { update(settings.copy(doublePage = it)) }
            Toggle(Res.string.reader_crop, Res.string.reader_crop_hint, settings.cropBorders) { update(settings.copy(cropBorders = it)) }
            Toggle(
                Res.string.reader_keep_on,
                Res.string.reader_keep_on_hint,
                settings.keepScreenOn,
            ) { update(settings.copy(keepScreenOn = it)) }
            Toggle(Res.string.reader_split, Res.string.reader_split_hint, settings.splitWide) { update(settings.copy(splitWide = it)) }
            Toggle(
                Res.string.reader_invert_pages,
                Res.string.reader_invert_pages_hint,
                settings.invertPages,
            ) { update(settings.copy(invertPages = it)) }
            Toggle(
                Res.string.reader_page_number,
                Res.string.reader_page_number_hint,
                settings.showPageNumber,
            ) { update(settings.copy(showPageNumber = it)) }
            Toggle(
                Res.string.reader_volume_keys,
                Res.string.reader_volume_keys_hint,
                settings.volumeKeys,
            ) { update(settings.copy(volumeKeys = it)) }
        }
    }
}

@Composable
private fun Label(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(
    title: StringResource,
    options: List<Pair<T, StringResource>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(title)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(label), maxLines = 1) }
            }
        }
    }
}

@Composable
private fun Toggle(
    title: StringResource,
    hint: StringResource,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
