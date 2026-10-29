package com.tiagohs.hqr.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.reader.ReaderBackground
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.reader.TapAction
import com.tiagohs.hqr.presentation.reader.TapZoneMap
import com.tiagohs.hqr.ui.components.ImageRef
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.chapter_short
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.reader_back_to_work
import com.tiagohs.hqr.ui.resources.reader_chapter_unavailable
import com.tiagohs.hqr.ui.resources.reader_end
import com.tiagohs.hqr.ui.resources.reader_gap
import com.tiagohs.hqr.ui.resources.reader_next
import com.tiagohs.hqr.ui.resources.reader_next_chapter
import com.tiagohs.hqr.ui.resources.reader_no_next
import com.tiagohs.hqr.ui.resources.reader_page_failed
import com.tiagohs.hqr.ui.resources.reader_page_of
import com.tiagohs.hqr.ui.resources.reader_previous_chapter
import com.tiagohs.hqr.ui.resources.reader_read_next
import com.tiagohs.hqr.ui.resources.reader_settings
import com.tiagohs.hqr.ui.resources.reader_swipe_continue
import com.tiagohs.hqr.ui.resources.reader_you_finished
import com.tiagohs.hqr.ui.theme.BalloonShape
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrTheme
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Pages kept composed on each side of the visible one, so their images load ahead (RF-41). */
private const val MAX_PRELOAD = 6

/** SCR-RDR-001: zero chrome until a center tap; the bars use the cover color (RF-36 to RF-43, RF-98). */
@Composable
fun ReaderScreen(
    mangaId: Long,
    chapterId: Long,
    startPage: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel(key = "reader-$mangaId-$chapterId") { parametersOf(mangaId, chapterId, startPage) },
    prefs: AppPreferences = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coverOn by prefs.coverColor.collectAsStateWithLifecycle()
    val seed = state.manga?.coverColor?.takeIf { coverOn } ?: CoverColor.VIOLET
    var settingsOpen by remember { mutableStateOf(false) }
    HqrTheme(dark = true, seed = seed) {
        val background =
            when (state.settings.background) {
                ReaderBackground.WHITE -> Color.White
                ReaderBackground.GRAY -> Color(0xFF3A3A3C)
                else -> Color.Black
            }
        Box(Modifier.fillMaxSize().background(background)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null ->
                    ReaderMessage(
                        stringResource(Res.string.error_title),
                        stringResource(state.error!!.message()),
                        stringResource(Res.string.action_retry),
                        viewModel::retry,
                        onBack,
                    )
                state.isEmptyChapter ->
                    ReaderMessage(
                        stringResource(Res.string.reader_chapter_unavailable),
                        "",
                        stringResource(Res.string.action_back),
                        onBack,
                        onBack,
                    )
                state.transition != null -> TransitionPage(state, viewModel, onBack)
                // A new chapter starts a new pager/list at its own page.
                else -> key(state.chapter?.id) { Pages(state, viewModel) }
            }
            AnimatedVisibility(state.menuVisible && state.transition == null && !state.loading, enter = fadeIn(), exit = fadeOut()) {
                ReaderMenu(state, viewModel, onBack, onSettings = { settingsOpen = true })
            }
            if (settingsOpen) {
                ReaderSettingsSheet(
                    settings = state.settings,
                    onChange = { s, only -> viewModel.updateSettings(s, only) },
                    onDismiss = { settingsOpen = false },
                )
            }
        }
    }
}

@Composable
private fun Pages(
    state: ReaderViewModel.State,
    viewModel: ReaderViewModel,
) {
    val settings = state.settings
    val rtl = settings.mode == ReaderMode.RIGHT_TO_LEFT
    // One extra item after the last page leads to the transition.
    val count = state.pageCount + 1
    val tap: (Float, Float, () -> Unit, () -> Unit) -> Unit = { x, y, prev, next ->
        when (TapZoneMap.action(settings.tapZones, settings.mode, settings.invertZones, x, y)) {
            TapAction.PREVIOUS -> prev()
            TapAction.NEXT -> next()
            TapAction.MENU -> viewModel.toggleMenu()
        }
    }
    when (settings.mode) {
        ReaderMode.WEBTOON -> {
            val list = rememberLazyListState(initialFirstVisibleItemIndex = state.page)
            val pageCount = state.pageCount
            LaunchedEffect(list) {
                // The page is the first visible one; the end is the bottom of the list with the last page on screen
                // (RF-43), even when the last pages fit on one screen.
                snapshotFlow {
                    val lastVisible =
                        list.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: 0
                    Triple(list.firstVisibleItemIndex, lastVisible, list.canScrollForward)
                }.collect { (first, lastVisible, canScroll) ->
                    if (!canScroll && lastVisible >= pageCount - 1 && list.layoutInfo.totalItemsCount > 0) {
                        viewModel.onPageChanged(pageCount - 1)
                        viewModel.onReachedEnd()
                    } else {
                        viewModel.onPageChanged(first.coerceAtMost(pageCount - 1))
                    }
                }
            }
            LaunchedEffect(state.page) {
                // The page slider moves the list too (RF-40).
                if (state.page != list.firstVisibleItemIndex && state.page < pageCount) list.scrollToItem(state.page)
            }
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { viewModel.toggleMenu() } },
            ) {
                itemsIndexed(state.pages, key = {
                    i,
                    _,
                    ->
                    i
                }) { index, page -> PageImage(state, index, page.imageUrl, page.headers, ContentScale.FillWidth) }
            }
        }
        else -> {
            val pager = rememberPagerState(initialPage = state.page) { count }
            val scope = rememberCoroutineScope()
            LaunchedEffect(pager) {
                snapshotFlow { pager.currentPage }.collect { index ->
                    if (index >= state.pageCount) viewModel.onReachedEnd() else viewModel.onPageChanged(index)
                }
            }
            LaunchedEffect(state.page) {
                if (state.page != pager.currentPage &&
                    state.page < state.pageCount
                ) {
                    pager.scrollToPage(state.page)
                }
            }
            val prev = {
                scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                Unit
            }
            val next = {
                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                Unit
            }
            val tapModifier =
                Modifier.fillMaxSize().pointerInput(settings) {
                    detectTapGestures { offset -> tap(offset.x / size.width, offset.y / size.height, prev, next) }
                }
            if (settings.mode == ReaderMode.VERTICAL) {
                VerticalPager(pager, modifier = tapModifier, beyondViewportPageCount = settings.preload.coerceIn(1, MAX_PRELOAD)) { index ->
                    if (index <
                        state.pageCount
                    ) {
                        PageImage(state, index, state.pages[index].imageUrl, state.pages[index].headers, ContentScale.Fit)
                    }
                }
            } else {
                HorizontalPager(
                    pager,
                    modifier = tapModifier,
                    reverseLayout = rtl,
                    beyondViewportPageCount = settings.preload.coerceIn(1, MAX_PRELOAD),
                ) { index ->
                    if (index <
                        state.pageCount
                    ) {
                        PageImage(state, index, state.pages[index].imageUrl, state.pages[index].headers, ContentScale.Fit)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageImage(
    state: ReaderViewModel.State,
    index: Int,
    url: String,
    headers: Map<String, String>,
    scale: ContentScale,
) {
    val manga = state.manga ?: return
    var attempt by remember(url) { mutableStateOf(0) }
    val label = stringResource(Res.string.reader_page_of, index + 1, state.pageCount)
    Box(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = ImageRef(manga.sourceId, url, headers + ("x-attempt" to attempt.toString())),
            contentDescription = null,
            contentScale = scale,
            modifier = if (scale == ContentScale.FillWidth) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
            loading = { Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            error = {
                // RF-51: only this page fails, with its own retry.
                Column(
                    Modifier.fillMaxWidth().height(400.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(Res.string.reader_page_failed), color = Color.White)
                    TextButton(onClick = { attempt++ }) { Text(stringResource(Res.string.action_retry)) }
                }
            },
        )
    }
}

@Composable
private fun ReaderMenu(
    state: ReaderViewModel.State,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val rtl = state.settings.mode == ReaderMode.RIGHT_TO_LEFT
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    colors.surfaceContainerLow.copy(alpha = 0.92f),
                ).statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back), tint = colors.onSurface) }
            Column(Modifier.weight(1f)) {
                Text(state.manga?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, maxLines = 1)
                val chapter = state.chapter
                if (chapter != null) {
                    Text(
                        listOfNotNull(
                            stringResource(Res.string.chapter_short, chapter.number.chapterLabel()),
                            chapter.source.title,
                            chapter.source.group,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .background(colors.surfaceContainerHighest, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // RF-40 and design.md: in right-to-left reading the bar and the chapter buttons are mirrored.
            val first = if (rtl) state.next else state.previous
            val last = if (rtl) state.previous else state.next
            IconButton(onClick = { if (rtl) viewModel.goToNext() else viewModel.goToPrevious() }, enabled = first != null) {
                Icon(
                    HqrIcons.PrevChapter,
                    stringResource(if (rtl) Res.string.reader_next_chapter else Res.string.reader_previous_chapter),
                    tint = colors.onSurface,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.reader_page_of, state.page + 1, state.pageCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Slider(
                    value = state.page.toFloat(),
                    onValueChange = { viewModel.onPageChanged(it.toInt()) },
                    valueRange = 0f..(state.pageCount - 1).coerceAtLeast(1).toFloat(),
                    steps = (state.pageCount - 2).coerceAtLeast(0),
                    modifier = if (rtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier,
                )
            }
            IconButton(onClick = { if (rtl) viewModel.goToPrevious() else viewModel.goToNext() }, enabled = last != null) {
                Icon(
                    HqrIcons.NextChapter,
                    stringResource(if (rtl) Res.string.reader_previous_chapter else Res.string.reader_next_chapter),
                    tint = colors.onSurface,
                )
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(48.dp).background(colors.primaryContainer, RoundedCornerShape(24.dp)),
            ) {
                Icon(HqrIcons.Tune, stringResource(Res.string.reader_settings), tint = colors.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun TransitionPage(
    state: ReaderViewModel.State,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
) {
    val transition = state.transition ?: return
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(Res.string.reader_end),
            fontFamily = bangers(),
            style = MaterialTheme.typography.displayMedium,
            color = Color(0xFF16161A),
            modifier =
                Modifier.background(Color(0xFFF6F1E7), BalloonShape).padding(horizontal = 28.dp, vertical = 10.dp).semantics {
                    contentDescription =
                        ""
                },
        )
        Text(stringResource(Res.string.reader_you_finished), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Text(
            stringResource(Res.string.chapter_short, transition.finished.number.chapterLabel()),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        if (transition.missing.isNotEmpty()) {
            Text(
                stringResource(Res.string.reader_gap, transition.missing.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer,
                modifier = Modifier.fillMaxWidth().background(colors.errorContainer, RoundedCornerShape(16.dp)).padding(14.dp),
            )
        }
        val next = transition.next
        if (next != null) {
            Column(
                Modifier.fillMaxWidth().background(colors.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(Res.string.reader_next), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(
                    listOfNotNull(
                        stringResource(Res.string.chapter_short, next.number.chapterLabel()),
                        next.source.title,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                if (next.source.group !=
                    null
                ) {
                    Text(next.source.group!!, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Text(
                stringResource(Res.string.reader_swipe_continue),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        } else {
            Text(stringResource(Res.string.reader_no_next), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(stringResource(Res.string.reader_back_to_work)) }
            if (next != null) {
                Button(onClick = viewModel::goToNext, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(Res.string.reader_read_next, next.number.chapterLabel()))
                }
            }
        }
        TextButton(onClick = viewModel::dismissTransition) { Text(stringResource(Res.string.action_back)) }
    }
}

@Composable
private fun ReaderMessage(
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
        if (message.isNotEmpty()) Text(message, color = Color(0xFFC9C4D3), textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text(stringResource(Res.string.action_back)) }
            Button(onClick = onAction) { Text(action) }
        }
    }
}
