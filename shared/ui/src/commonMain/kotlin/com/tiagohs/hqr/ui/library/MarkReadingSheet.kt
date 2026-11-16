package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.ReadingMark
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.presentation.manga.MarkReadingViewModel
import com.tiagohs.hqr.presentation.manga.MarkReadingViewModel.State
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.action_save
import com.tiagohs.hqr.ui.resources.action_undo
import com.tiagohs.hqr.ui.resources.chapter_short
import com.tiagohs.hqr.ui.resources.mark_reading
import com.tiagohs.hqr.ui.resources.mark_reading_all
import com.tiagohs.hqr.ui.resources.mark_reading_all_hint
import com.tiagohs.hqr.ui.resources.mark_reading_chapter
import com.tiagohs.hqr.ui.resources.mark_reading_chapters
import com.tiagohs.hqr.ui.resources.mark_reading_done_all
import com.tiagohs.hqr.ui.resources.mark_reading_done_none
import com.tiagohs.hqr.ui.resources.mark_reading_done_up_to
import com.tiagohs.hqr.ui.resources.mark_reading_loading
import com.tiagohs.hqr.ui.resources.mark_reading_next
import com.tiagohs.hqr.ui.resources.mark_reading_none
import com.tiagohs.hqr.ui.resources.mark_reading_note
import com.tiagohs.hqr.ui.resources.mark_reading_previous
import com.tiagohs.hqr.ui.resources.mark_reading_slider
import com.tiagohs.hqr.ui.resources.mark_reading_up_to
import com.tiagohs.hqr.ui.resources.mark_reading_up_to_hint
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun rememberMarkReadingHost(): MarkReadingHost = remember { MarkReadingHost() }

/** Shows the sheet of [host]'s work and, once saved, the result with "Desfazer" in [snackbar]. */
@Composable
fun MarkReadingSheetHost(
    host: MarkReadingHost,
    snackbar: SnackbarHostState,
) {
    // Before the sheet's own state: the message and "Desfazer" outlive the sheet.
    val scope = rememberCoroutineScope()
    val id = host.mangaId ?: return
    val viewModel: MarkReadingViewModel = koinViewModel(key = "mark-$id") { parametersOf(id) }
    MarkReadingSheet(
        viewModel = viewModel,
        onDismiss = host::close,
        onSaved = { saved ->
            host.close()
            scope.launch {
                val text =
                    when (saved.mark) {
                        ReadingMark.None -> getString(Res.string.mark_reading_done_none, saved.title)
                        ReadingMark.All -> getString(Res.string.mark_reading_done_all, saved.title)
                        is ReadingMark.UpTo ->
                            getString(
                                Res.string.mark_reading_done_up_to,
                                saved.title,
                                saved.chapterNumber?.chapterLabel().orEmpty(),
                            )
                    }
                val result = snackbar.showSnackbar(text, getString(Res.string.action_undo), duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) viewModel.undo()
            }
        },
    )
}

/** SCR-LIB-004: none, up to a chapter, or all; a work without its chapter list asks its source first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkReadingSheet(
    viewModel: MarkReadingViewModel,
    onDismiss: () -> Unit,
    onSaved: (MarkReadingViewModel.Saved) -> Unit,
    sourceName: String = "",
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The whole sheet at once: "Salvar" is never below the fold.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(Res.string.mark_reading),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
            )
            val ready = state as? State.Ready
            Text(
                when {
                    ready != null -> stringResource(Res.string.mark_reading_chapters, ready.title, ready.total)
                    state is State.Loading -> stringResource(Res.string.mark_reading_loading, sourceName.ifBlank { state.title })
                    else -> state.title
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            when (val current = state) {
                is State.Failed ->
                    InlineError(
                        message = stringResource(current.error.message()),
                        modifier = Modifier.padding(16.dp),
                        action = stringResource(Res.string.action_retry),
                        onAction = viewModel::retry,
                    )
                else -> Options(current as? State.Ready, viewModel)
            }
            Text(
                stringResource(Res.string.mark_reading_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
                Button(onClick = { viewModel.save(onSaved) }, enabled = ready != null) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}

@Composable
private fun Options(
    ready: State.Ready?,
    viewModel: MarkReadingViewModel,
) {
    val mark = ready?.mark ?: ReadingMark.None
    Column(Modifier.selectableGroup()) {
        Option(stringResource(Res.string.mark_reading_none), null, mark == ReadingMark.None, ready != null) {
            viewModel.choose(ReadingMark.None)
        }
        Option(
            stringResource(Res.string.mark_reading_up_to),
            stringResource(Res.string.mark_reading_up_to_hint),
            mark is ReadingMark.UpTo,
            ready != null && ready.chapters.isNotEmpty(),
        ) { ready?.let { viewModel.chooseUpTo(it.index) } }
        if (ready != null && ready.chapters.isNotEmpty()) ChapterPicker(ready, viewModel)
        Option(
            stringResource(Res.string.mark_reading_all),
            ready?.let { stringResource(Res.string.mark_reading_all_hint, it.total) },
            mark == ReadingMark.All,
            ready != null,
        ) { viewModel.choose(ReadingMark.All) }
    }
}

@Composable
private fun Option(
    label: String,
    hint: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = hint?.let { { Text(it) } },
        leadingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    )
}

/** The chapter for "lido até": − and + for one step, the slider for the rest (RF-126). */
@Composable
private fun ChapterPicker(
    ready: State.Ready,
    viewModel: MarkReadingViewModel,
) {
    val last = ready.chapters.lastIndex
    val chapter = ready.chapter ?: return
    val number = chapter.number.chapterLabel()
    val active = ready.mark is ReadingMark.UpTo
    Column(Modifier.padding(start = 72.dp, end = 24.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedIconButton(onClick = { viewModel.chooseUpTo(ready.index - 1) }, enabled = ready.index > 0) {
                Icon(HqrIcons.Minus, stringResource(Res.string.mark_reading_previous))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.mark_reading_chapter, number),
                    fontFamily = bangers(),
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                chapter.source.title?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            OutlinedIconButton(onClick = { viewModel.chooseUpTo(ready.index + 1) }, enabled = ready.index < last) {
                Icon(HqrIcons.Add, stringResource(Res.string.mark_reading_next))
            }
        }
        if (last > 0) {
            val sliderLabel = stringResource(Res.string.mark_reading_slider)
            val valueLabel = stringResource(Res.string.chapter_short, number)
            Slider(
                value = ready.index.toFloat(),
                onValueChange = { viewModel.chooseUpTo(kotlin.math.round(it).toInt()) },
                valueRange = 0f..last.toFloat(),
                modifier =
                    Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = sliderLabel
                        stateDescription = valueLabel
                    },
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        Res.string.chapter_short,
                        ready.chapters
                            .first()
                            .number
                            .chapterLabel(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(
                        Res.string.chapter_short,
                        ready.chapters
                            .last()
                            .number
                            .chapterLabel(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp).width(1.dp))
    }
}
