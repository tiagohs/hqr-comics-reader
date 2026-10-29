package com.tiagohs.hqr.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.EmptyState
import com.tiagohs.hqr.ui.components.HalftoneProgress
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.UnreadBalloon
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_continue
import com.tiagohs.hqr.ui.resources.action_search
import com.tiagohs.hqr.ui.resources.library_continue_reading
import com.tiagohs.hqr.ui.resources.library_empty_message
import com.tiagohs.hqr.ui.resources.library_empty_title
import com.tiagohs.hqr.ui.resources.library_tab_all
import com.tiagohs.hqr.ui.resources.library_tab_uncategorized
import com.tiagohs.hqr.ui.resources.library_unread
import com.tiagohs.hqr.ui.resources.library_up_to_date
import com.tiagohs.hqr.ui.resources.manga_continue_chapter
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.nav_library
import com.tiagohs.hqr.ui.resources.onomatopoeia_empty
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrShapes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** SCR-LIB-001: "Continuar lendo", category tabs and the grid (RF-52 to RF-54). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenManga: (Long) -> Unit,
    onContinue: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    onExplore: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_library), style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onExplore) { Icon(HqrIcons.Search, stringResource(Res.string.action_search)) }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.isEmpty ->
                EmptyState(
                    title = stringResource(Res.string.library_empty_title),
                    message = stringResource(Res.string.library_empty_message),
                    onomatopoeia = stringResource(Res.string.onomatopoeia_empty),
                    action = stringResource(Res.string.nav_explore),
                    onAction = onExplore,
                    modifier = Modifier.padding(padding),
                )
            else ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    if (state.tabs.size > 1) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = state.tabs.indexOfFirst { it.id == state.selectedTab }.coerceAtLeast(0),
                            edgePadding = 8.dp,
                        ) {
                            state.tabs.forEach { tab ->
                                Tab(
                                    selected = tab.id == state.selectedTab,
                                    onClick = { viewModel.selectTab(tab.id) },
                                    text = {
                                        val name =
                                            when (tab.id) {
                                                LibraryViewModel.ALL -> stringResource(Res.string.library_tab_all)
                                                LibraryViewModel.UNCATEGORIZED -> stringResource(Res.string.library_tab_uncategorized)
                                                else -> tab.name
                                            }
                                        Text("$name ${tab.count}")
                                    },
                                )
                            }
                        }
                    }
                    LibraryGrid(state.items, state.continueReading, onOpenManga, onContinue)
                }
        }
    }
}

@Composable
private fun LibraryGrid(
    items: List<LibraryItem>,
    continueReading: HistoryEntry?,
    onOpenManga: (Long) -> Unit,
    onContinue: (Long, Long, Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (continueReading != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { ContinueCard(continueReading, onContinue) }
        }
        items(items, key = { it.manga.id }) { item ->
            val unreadLabel =
                if (item.unread >
                    0
                ) {
                    stringResource(Res.string.library_unread, item.unread)
                } else {
                    stringResource(Res.string.library_up_to_date)
                }
            Column(
                Modifier
                    .clickable { onOpenManga(item.manga.id) }
                    .semantics(mergeDescendants = true) { contentDescription = "${item.manga.title}, $unreadLabel" },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Cover(item.manga.source) { UnreadBalloon(item.unread) }
                Text(item.manga.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(unreadLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ContinueCard(
    entry: HistoryEntry,
    onContinue: (Long, Long, Int) -> Unit,
) {
    val chapter = entry.chapter
    val total = chapter.pageCount ?: 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(68.dp)) { Cover(entry.manga.source, shape = HqrShapes.coverSmall) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.library_continue_reading),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(entry.manga.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(Res.string.manga_continue_chapter, chapter.number.chapterLabel(), chapter.lastPage + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (total > 0) HalftoneProgress((chapter.lastPage + 1f) / total)
        }
        Button(onClick = { onContinue(entry.manga.id, chapter.id, RESUME) }) { Text(stringResource(Res.string.action_continue)) }
    }
}
