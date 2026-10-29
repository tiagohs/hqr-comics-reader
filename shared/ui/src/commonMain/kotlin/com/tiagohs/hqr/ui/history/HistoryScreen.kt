package com.tiagohs.hqr.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_cancel
import com.tiagohs.hqr.ui.resources.history_clear
import com.tiagohs.hqr.ui.resources.history_clear_confirm
import com.tiagohs.hqr.ui.resources.history_empty_message
import com.tiagohs.hqr.ui.resources.history_empty_title
import com.tiagohs.hqr.ui.resources.history_remove
import com.tiagohs.hqr.ui.resources.history_today
import com.tiagohs.hqr.ui.resources.history_yesterday
import com.tiagohs.hqr.ui.resources.manga_page_of
import com.tiagohs.hqr.ui.resources.nav_history
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrShapes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-HIS-001: by day; a tap resumes on the saved page (RF-59, RF-60). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onResume: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_history), style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    if (!state.isEmpty) {
                        IconButton(
                            onClick = { confirm = true },
                        ) { Icon(HqrIcons.Delete, stringResource(Res.string.history_clear)) }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.isEmpty ->
                EmptyState(
                    stringResource(Res.string.history_empty_title),
                    stringResource(Res.string.history_empty_message),
                    Modifier.padding(padding),
                )
            else ->
                LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                    state.days.forEach { day ->
                        item(key = "day-${day.date}") {
                            val label =
                                when {
                                    day.isToday -> stringResource(Res.string.history_today)
                                    day.isYesterday -> stringResource(Res.string.history_yesterday)
                                    else -> "${day.date.day.toString().padStart(
                                        2,
                                        '0',
                                    )}/${day.date.month.ordinal.plus(1).toString().padStart(2, '0')}"
                                }
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(day.entries, key = { "${day.date}-${it.chapter.id}" }) { entry ->
                            val chapter = entry.chapter
                            ListItem(
                                headlineContent = { Text(entry.manga.title, maxLines = 1) },
                                supportingContent = {
                                    val page = chapter.pageCount?.let { stringResource(Res.string.manga_page_of, chapter.lastPage + 1, it) }
                                    Text(listOfNotNull("Cap. " + chapter.number.chapterLabel(), page).joinToString(" · "))
                                },
                                leadingContent = { Box(Modifier.width(48.dp)) { Cover(entry.manga.source, shape = HqrShapes.coverSmall) } },
                                trailingContent = {
                                    IconButton(
                                        onClick = { viewModel.remove(entry) },
                                    ) { Icon(HqrIcons.Close, stringResource(Res.string.history_remove)) }
                                },
                                modifier =
                                    Modifier.clickable {
                                        onResume(
                                            entry.manga.id,
                                            chapter.id,
                                            if (chapter.read) 0 else chapter.lastPage,
                                        )
                                    },
                            )
                        }
                    }
                }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(Res.string.history_clear)) },
            text = { Text(stringResource(Res.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.clearAll()
                }) { Text(stringResource(Res.string.history_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}
