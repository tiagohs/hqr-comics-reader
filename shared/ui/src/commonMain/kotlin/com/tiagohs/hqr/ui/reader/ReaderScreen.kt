package com.tiagohs.hqr.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.reader.KeyAction
import com.tiagohs.hqr.presentation.reader.PageScale
import com.tiagohs.hqr.presentation.reader.PageView
import com.tiagohs.hqr.presentation.reader.ReaderBackground
import com.tiagohs.hqr.presentation.reader.ReaderLayout
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.reader.TapAction
import com.tiagohs.hqr.presentation.reader.TapZoneMap
import com.tiagohs.hqr.ui.components.ImageRef
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_open_browser
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.chapter_short
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.manga_external
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Pages kept composed on each side of the visible one, so their images load ahead (RF-41). */
private const val MAX_PRELOAD = 6
private const val MAX_SLIDER_TICKS = 30
private const val MAX_ZOOM = 4f
private const val SWIPE_PX = 120f

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
    var shortcutsOpen by remember { mutableStateOf(false) }
    // Keys and controller buttons go to the pages, which know whether a screen is one page or two (RF-44, RF-47).
    val commands = remember { Channel<KeyAction>(Channel.CONFLATED) }
    // RF-46: the screen stays on while reading, and the bars go away with the controls (camera cutout used).
    KeepScreenOn(state.settings.keepScreenOn)
    ImmersiveMode(hideBars = !state.menuVisible)
    HqrTheme(dark = true, seed = seed) {
        val background =
            when (state.settings.background) {
                ReaderBackground.WHITE -> Color.White
                ReaderBackground.GRAY -> Color(0xFF3A3A3C)
                else -> Color.Black
            }
        Box(
            Modifier
                .fillMaxSize()
                .background(background)
                .readerInput(
                    mode = state.settings.mode,
                    volumeKeys = state.settings.volumeKeys,
                    menuVisible = state.menuVisible,
                    overlayOpen = shortcutsOpen || settingsOpen,
                    onAction = { action ->
                        when (action) {
                            KeyAction.FULLSCREEN -> toggleFullscreen()
                            KeyAction.SHORTCUTS -> shortcutsOpen = true
                            KeyAction.CLOSE -> onBack()
                            // Keys pressed on the transition or while a chapter loads are not kept for later.
                            else -> if (state.transition == null && !state.loading && state.pages.isNotEmpty()) commands.trySend(action)
                        }
                    },
                    onMouseMoved = viewModel::showMenu,
                    onMouseIdle = viewModel::hideMenu,
                ),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                // RF-11: a chapter only on the publisher's site (reached by "Continuar" or the next chapter) opens there.
                state.error != null && state.chapter?.source?.externalUrl != null -> {
                    val uriHandler = LocalUriHandler.current
                    ReaderMessage(
                        stringResource(Res.string.reader_chapter_unavailable),
                        stringResource(Res.string.manga_external),
                        stringResource(Res.string.action_open_browser),
                        {
                            state.chapter
                                ?.source
                                ?.externalUrl
                                ?.let(uriHandler::openUri)
                        },
                        onBack,
                    )
                }
                state.error != null ->
                    ReaderMessage(
                        stringResource(Res.string.error_title),
                        stringResource(state.error!!.message()),
                        stringResource(Res.string.action_retry),
                        viewModel::retry,
                        onBack,
                    )
                state.isEmptyChapter -> {
                    // RF-51: "Back" and "Open in browser" (the chapter's own page, else the work's).
                    val uriHandler = LocalUriHandler.current
                    val url = state.chapter?.source?.externalUrl ?: state.manga?.source?.url
                    ReaderMessage(
                        stringResource(Res.string.reader_chapter_unavailable),
                        "",
                        stringResource(Res.string.action_open_browser),
                        { url?.let(uriHandler::openUri) ?: onBack() },
                        onBack,
                    )
                }
                state.transition != null -> TransitionPage(state, viewModel, onBack)
                // A new chapter starts a new pager/list at its own page.
                else -> key(state.chapter?.id) { Pages(state, viewModel, commands) }
            }
            AnimatedVisibility(state.menuVisible && state.transition == null && !state.loading, enter = fadeIn(), exit = fadeOut()) {
                ReaderMenu(state, viewModel, onBack, onSettings = { settingsOpen = true })
            }
            // RF-46: the page number, while the controls are hidden.
            val reading = state.pageCount > 0 && state.transition == null && !state.loading
            if (state.settings.showPageNumber && !state.menuVisible && reading) {
                PageNumber(state.page + 1, state.pageCount, Modifier.align(Alignment.BottomCenter))
            }
            if (shortcutsOpen) ShortcutsDialog(onDismiss = { shortcutsOpen = false })
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
    commands: Channel<KeyAction>,
) {
    val settings = state.settings
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
            // RF-112: in the webtoon the keys move page by page through the list.
            LaunchedEffect(list) { for (command in commands) viewModel.key(command) }
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
                }) { index, _ -> PageImage(state, index, ContentScale.FillWidth, viewModel) }
            }
        }
        else -> PagedPages(state, viewModel, commands, tap)
    }
}

/** RF-36, RF-44: the paged modes; each screen is one page, a spread or half of a wide page. */
@Composable
private fun PagedPages(
    state: ReaderViewModel.State,
    viewModel: ReaderViewModel,
    commands: Channel<KeyAction>,
    tap: (Float, Float, () -> Unit, () -> Unit) -> Unit,
) {
    val settings = state.settings
    val rtl = settings.mode == ReaderMode.RIGHT_TO_LEFT
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val sideways = settings.mode != ReaderMode.VERTICAL
        val double = settings.doublePage && landscape && sideways
        val split = settings.splitWide && !landscape && sideways
        val views =
            remember(state.pageCount, state.wide, double, split) {
                ReaderLayout.views(count = state.pageCount, wide = state.wide, double = double, split = split)
            }
        // Turning the phone changes the layout: a new pager starts on the same page, never at an old screen number.
        key(double, split) {
            // One extra screen after the last leads to the transition.
            val pager = rememberPagerState(initialPage = ReaderLayout.viewOf(views, state.page)) { views.size + 1 }
            val scope = rememberCoroutineScope()
            val latestViews by rememberUpdatedState(views)
            LaunchedEffect(pager) {
                // The settled screen: half a swipe on the last page never opens the transition (RF-42). A wide page
                // found later changes the screens without moving the pager; the effect below re-anchors it first.
                snapshotFlow { pager.settledPage }.collect { index ->
                    val current = latestViews
                    if (index >= current.size) viewModel.onReachedEnd() else viewModel.onPageChanged(current[index].pages.first())
                }
            }
            LaunchedEffect(state.page, views) {
                val target = ReaderLayout.viewOf(views, state.page)
                // A spread or the second half already shows the page: only another page moves the pager.
                if (state.page !in views.getOrNull(pager.currentPage)?.pages.orEmpty() && state.page < state.pageCount) {
                    pager.scrollToPage(target)
                }
            }
            LaunchedEffect(pager) {
                // RF-47, RF-112: keys and buttons move by screen, so a spread turns as one.
                for (command in commands) {
                    val views = latestViews
                    val to =
                        when (command) {
                            KeyAction.NEXT -> pager.currentPage + 1
                            KeyAction.PREVIOUS -> pager.currentPage - 1
                            KeyAction.FIRST -> 0
                            KeyAction.LAST -> views.size - 1
                            else -> continue
                        }
                    pager.animateScrollToPage(to.coerceIn(0, views.size))
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
            val onTap: (Float, Float) -> Unit = { x, y -> tap(x, y, prev, next) }
            val scale = settings.scale.toContentScale()
            val preload = settings.preload.coerceIn(1, MAX_PRELOAD)
            val firstOnLeft = ReaderLayout.firstOnLeft(settings.mode, settings.invertPages)
            val screen: @Composable (Int) -> Unit = { index ->
                when (val view = views.getOrNull(index)) {
                    null -> Box(Modifier.fillMaxSize())
                    is PageView.Single ->
                        ZoomablePage(onTap) {
                            // RF-39: a tall page fitted to the width (or at its size) scrolls down inside the page.
                            val scroll = scale == ContentScale.FillWidth || scale == ContentScale.None
                            Box(if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()) {
                                PageImage(state, view.index, scale, viewModel)
                            }
                        }
                    is PageView.Spread ->
                        ZoomablePage(onTap) {
                            val (left, right) = if (firstOnLeft) view.first to view.second else view.second to view.first
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                                    PageImage(state, left, ContentScale.Fit, viewModel, Alignment.CenterEnd)
                                }
                                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                    PageImage(state, right, ContentScale.Fit, viewModel, Alignment.CenterStart)
                                }
                            }
                        }
                    is PageView.Half ->
                        ZoomablePage(onTap) {
                            // The wide page at twice the screen's width, moved so only the half being read shows.
                            val showLeft = (view.part == 0) == firstOnLeft
                            Box(Modifier.fillMaxSize().clipToBounds()) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .layout { measurable, constraints ->
                                            val full = constraints.maxWidth * 2
                                            val placeable = measurable.measure(constraints.copy(minWidth = full, maxWidth = full))
                                            layout(constraints.maxWidth, placeable.height) {
                                                placeable.place(if (showLeft) 0 else -constraints.maxWidth, 0)
                                            }
                                        },
                                ) { PageImage(state, view.index, ContentScale.Fit, viewModel) }
                            }
                        }
                }
            }
            if (settings.mode == ReaderMode.VERTICAL) {
                VerticalPager(pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = preload) { screen(it) }
            } else {
                HorizontalPager(
                    pager,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = rtl,
                    beyondViewportPageCount = preload,
                ) { screen(it) }
            }
        }
    }
}

/** RF-39: pinch and double tap zoom on each page; a single tap goes to the tap zones (RF-38). */
@Composable
private fun ZoomablePage(
    onTap: (Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val zoom = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = MAX_ZOOM))
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .zoomable(zoom, onClick = { offset -> if (size.width > 0) onTap(offset.x / size.width, offset.y / size.height) }),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun PageScale.toContentScale(): ContentScale =
    when (this) {
        PageScale.FIT_SCREEN -> ContentScale.Fit
        PageScale.FIT_WIDTH -> ContentScale.FillWidth
        PageScale.FIT_HEIGHT -> ContentScale.FillHeight
        PageScale.ORIGINAL -> ContentScale.None
    }

@Composable
private fun PageImage(
    state: ReaderViewModel.State,
    index: Int,
    scale: ContentScale,
    viewModel: ReaderViewModel,
    alignment: Alignment = Alignment.Center,
) {
    val manga = state.manga ?: return
    val page = state.pages.getOrNull(index) ?: return
    var attempt by remember(page.imageUrl) { mutableStateOf(0) }
    val label = stringResource(Res.string.reader_page_of, index + 1, state.pageCount)
    val context = LocalPlatformContext.current
    val crop = state.settings.cropBorders
    val request =
        remember(page.imageUrl, attempt, crop) {
            ImageRequest
                .Builder(context)
                .data(ImageRef(manga.sourceId, page.imageUrl, page.headers + ("x-attempt" to attempt.toString())))
                // RF-45: uniform white or black margins cut off.
                .apply { if (crop) transformations(CropBordersTransformation) }
                .build()
        }
    Box(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        contentAlignment = alignment,
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            contentScale = scale,
            alignment = alignment,
            // RF-44: a page wider than tall is shown alone (or split in portrait).
            onSuccess = { success ->
                val size = success.painter.intrinsicSize
                if (size.isSpecified) viewModel.onPageSize(index, size.width, size.height)
            },
            modifier =
                when (scale) {
                    ContentScale.FillWidth -> Modifier.fillMaxWidth()
                    ContentScale.None -> Modifier.wrapContentSize()
                    else -> Modifier.fillMaxSize()
                },
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
            var dragging by remember { mutableStateOf<Float?>(null) }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        Res.string.reader_page_of,
                        ((dragging ?: state.page.toFloat()).let { kotlin.math.round(it).toInt() }) + 1,
                        state.pageCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                // RF-40: the bar shows the page while dragging and turns to it on release; right to left it runs from the
                // right, with the arrow keys following.
                val sliderLabel = stringResource(Res.string.reader_page_of, state.page + 1, state.pageCount)
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Slider(
                        value = dragging ?: state.page.toFloat(),
                        onValueChange = { dragging = it },
                        onValueChangeFinished = {
                            dragging?.let { viewModel.onPageChanged(kotlin.math.round(it).toInt()) }
                            dragging = null
                        },
                        valueRange = 0f..(state.pageCount - 1).coerceAtLeast(1).toFloat(),
                        // Tick marks only while they stay readable; long chapters slide freely and snap to the page.
                        steps = if (state.pageCount <= MAX_SLIDER_TICKS) (state.pageCount - 2).coerceAtLeast(0) else 0,
                        modifier = Modifier.semantics { contentDescription = sliderLabel },
                    )
                }
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
    val rtl = state.settings.mode == ReaderMode.RIGHT_TO_LEFT
    var drag by remember { mutableStateOf(0f) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // "Arraste para continuar": a swipe forward opens the next chapter, backward returns to the last page.
            .pointerInput(rtl) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        val forward = if (rtl) drag > SWIPE_PX else drag < -SWIPE_PX
                        val backward = if (rtl) drag < -SWIPE_PX else drag > SWIPE_PX
                        when {
                            forward && transition.next != null -> viewModel.goToNext()
                            backward -> viewModel.dismissTransition()
                        }
                    },
                )
            }.statusBarsPadding()
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
                Modifier.background(Color(0xFFF6F1E7), BalloonShape).padding(horizontal = 28.dp, vertical = 10.dp).clearAndSetSemantics {},
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

@Composable
private fun PageNumber(
    page: Int,
    count: Int,
    modifier: Modifier,
) {
    Text(
        "$page / $count",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .clearAndSetSemantics { },
    )
}
