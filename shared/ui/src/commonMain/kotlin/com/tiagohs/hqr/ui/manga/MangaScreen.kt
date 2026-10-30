package com.tiagohs.hqr.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadAction
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.ui.components.Cover
import com.tiagohs.hqr.ui.components.ErrorState
import com.tiagohs.hqr.ui.components.ImageRef
import com.tiagohs.hqr.ui.components.InlineError
import com.tiagohs.hqr.ui.components.LoadingState
import com.tiagohs.hqr.ui.components.label
import com.tiagohs.hqr.ui.components.message
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.navigation.RESUME
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.action_back
import com.tiagohs.hqr.ui.resources.action_continue
import com.tiagohs.hqr.ui.resources.action_less_text
import com.tiagohs.hqr.ui.resources.action_more
import com.tiagohs.hqr.ui.resources.action_more_text
import com.tiagohs.hqr.ui.resources.action_read
import com.tiagohs.hqr.ui.resources.action_read_again
import com.tiagohs.hqr.ui.resources.action_retry
import com.tiagohs.hqr.ui.resources.date_short
import com.tiagohs.hqr.ui.resources.error_title
import com.tiagohs.hqr.ui.resources.manga_add_library
import com.tiagohs.hqr.ui.resources.manga_chapter_label
import com.tiagohs.hqr.ui.resources.manga_chapters
import com.tiagohs.hqr.ui.resources.manga_continue_chapter
import com.tiagohs.hqr.ui.resources.manga_details
import com.tiagohs.hqr.ui.resources.manga_filter_all
import com.tiagohs.hqr.ui.resources.manga_filter_unread
import com.tiagohs.hqr.ui.resources.manga_in_library
import com.tiagohs.hqr.ui.resources.manga_mark_previous
import com.tiagohs.hqr.ui.resources.manga_mark_read
import com.tiagohs.hqr.ui.resources.manga_mark_unread
import com.tiagohs.hqr.ui.resources.manga_new
import com.tiagohs.hqr.ui.resources.manga_no_chapters
import com.tiagohs.hqr.ui.resources.manga_open_group
import com.tiagohs.hqr.ui.resources.manga_open_site
import com.tiagohs.hqr.ui.resources.manga_order
import com.tiagohs.hqr.ui.resources.manga_page_of
import com.tiagohs.hqr.ui.resources.manga_read
import com.tiagohs.hqr.ui.resources.manga_refresh_failed
import com.tiagohs.hqr.ui.resources.manga_removed_from_source
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.HqrTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

/** SCR-OBR-001: the work, in the colors of its cover (RF-29 to RF-33, RF-98). */
@Composable
fun MangaScreen(
    mangaId: Long,
    dark: Boolean,
    onBack: () -> Unit,
    onRead: (mangaId: Long, chapterId: Long, page: Int) -> Unit,
    viewModel: MangaViewModel = koinViewModel(key = "manga-$mangaId") { parametersOf(mangaId) },
    prefs: AppPreferences = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coverColorOn by prefs.coverColor.collectAsStateWithLifecycle()
    val seed = state.manga?.coverColor?.takeIf { coverColorOn } ?: CoverColor.VIOLET
    HqrTheme(dark = dark, seed = seed) {
        val manga = state.manga
        if (manga == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val error = state.error
                if (error == null) {
                    LoadingState()
                } else {
                    ErrorState(
                        stringResource(Res.string.error_title),
                        stringResource(error.message()),
                        stringResource(Res.string.action_back),
                        onBack,
                    )
                }
            }
        } else {
            MangaContent(manga, state, dark, viewModel, onBack, onRead)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaContent(
    manga: Manga,
    state: MangaViewModel.State,
    dark: Boolean,
    viewModel: MangaViewModel,
    onBack: () -> Unit,
    onRead: (Long, Long, Int) -> Unit,
) {
    val uri = LocalUriHandler.current
    val isLocal = manga.sourceId == SourceId.LOCAL
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(HqrIcons.Back, stringResource(Res.string.action_back)) } },
                actions = {
                    IconButton(onClick = {
                        viewModel.setOrder(
                            if (state.order ==
                                MangaViewModel.Order.NEWEST_FIRST
                            ) {
                                MangaViewModel.Order.OLDEST_FIRST
                            } else {
                                MangaViewModel.Order.NEWEST_FIRST
                            },
                        )
                    }) { Icon(HqrIcons.Sort, stringResource(Res.string.manga_order)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Hero(manga, dark)
                LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                    item { Header(manga) }
                    item {
                        Actions(
                            manga = manga,
                            action = state.action,
                            isLocal = isLocal,
                            onRead = { chapter, page -> onRead(manga.id, chapter.id, page) },
                            onToggleLibrary = viewModel::toggleLibrary,
                            onOpenSite = { manga.source.url?.let { uri.openUri(it) } },
                        )
                    }
                    item { Synopsis(manga.source.description, manga.source.genres, manga.source.alternativeTitles) }
                    if (state.error != null) {
                        item {
                            InlineError(
                                stringResource(Res.string.manga_refresh_failed),
                                action = stringResource(Res.string.action_retry),
                                onAction = viewModel::refresh,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.manga_chapters, state.totalChapters),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        ChapterFilters(state, viewModel)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (state.totalChapters == 0 && !state.refreshing && state.error == null) {
                        item {
                            Text(
                                stringResource(Res.string.manga_no_chapters),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(state.chapters, key = { it.id }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            isNew = chapter.id in state.newChapterIds,
                            onClick = { onRead(manga.id, chapter.id, if (chapter.read) 0 else RESUME) },
                            onMarkRead = { viewModel.setRead(listOf(chapter.id), !chapter.read) },
                            onMarkPrevious = { viewModel.markPreviousAsRead(chapter) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(
    manga: Manga,
    dark: Boolean,
) {
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        manga.source.coverUrl?.let {
            AsyncImage(
                model = ImageRef(manga.sourceId, it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (dark) 0.9f else 0.55f,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(if (dark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f), background),
                    endY =
                        300f * 3,
                ),
            ),
        )
    }
}

@Composable
private fun Header(manga: Manga) {
    val source = manga.source
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(116.dp)) { Cover(source) }
        Column(Modifier.weight(1f).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(source.title, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (source.alternativeTitles.isNotEmpty()) {
                // RF-29: alternative titles, one line; the full list is in the synopsis when expanded.
                Text(
                    source.alternativeTitles.take(2).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val people = (source.authors + source.artists).distinct()
            if (people.isNotEmpty()) {
                Text(
                    people.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val status = source.status.label()?.let { stringResource(it) }
            val sourceLine = listOfNotNull(status, sourceName(manga), source.language?.uppercase()).joinToString(" · ")
            Text(sourceLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun sourceName(manga: Manga): String =
    sourceDisplayName(
        manga.sourceId,
        koinInject<com.tiagohs.hqr.domain.source.SourceRegistry>()[manga.sourceId]?.name ?: manga.sourceId.value,
    )

@Composable
private fun Actions(
    manga: Manga,
    action: ReadAction,
    isLocal: Boolean,
    onRead: (Chapter, Int) -> Unit,
    onToggleLibrary: () -> Unit,
    onOpenSite: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val (label, detail, target) =
            when (action) {
                is ReadAction.Start -> Triple(stringResource(Res.string.action_read), null, action.chapter to 0)
                is ReadAction.Continue ->
                    Triple(
                        stringResource(Res.string.action_continue),
                        stringResource(Res.string.manga_continue_chapter, action.chapter.number.chapterLabel(), action.page + 1),
                        action.chapter to RESUME,
                    )
                is ReadAction.ReadAgain -> Triple(stringResource(Res.string.action_read_again), null, action.chapter to 0)
                ReadAction.Nothing -> Triple(stringResource(Res.string.action_read), null, null)
            }
        Button(
            onClick = { target?.let { onRead(it.first, it.second) } },
            enabled = target != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (detail != null) Text("  $detail", style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val libraryLabel = stringResource(if (manga.inLibrary) Res.string.manga_in_library else Res.string.manga_add_library)
            if (manga.inLibrary) {
                Button(
                    onClick = onToggleLibrary,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp, 8.dp, 8.dp, 24.dp),
                ) {
                    Icon(HqrIcons.Bookmark, null, Modifier.size(18.dp))
                    Text(" $libraryLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                FilledTonalButton(
                    onClick = onToggleLibrary,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp, 8.dp, 8.dp, 24.dp),
                ) {
                    Icon(HqrIcons.BookmarkOutline, null, Modifier.size(18.dp))
                    Text(" $libraryLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!isLocal && manga.source.url != null) {
                FilledTonalButton(
                    onClick = onOpenSite,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp, 24.dp, 24.dp, 8.dp),
                ) {
                    Icon(HqrIcons.Globe, null, Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.manga_open_site), maxLines = 1)
                }
            } else if (isLocal) {
                FilledTonalButton(onClick = {
                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp, 24.dp, 24.dp, 8.dp)) {
                    Icon(HqrIcons.Info, null, Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.manga_details), maxLines = 1)
                }
            }
        }
    }
}

private const val MAX_GENRES = 8

/** RF-30: unread and scan group filters above the chapter list. */
@Composable
private fun ChapterFilters(
    state: MangaViewModel.State,
    viewModel: MangaViewModel,
) {
    if (state.totalChapters == 0) return
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        item {
            androidx.compose.material3.FilterChip(
                selected = state.filter == MangaViewModel.ChapterFilter.ALL && state.group == null,
                onClick = {
                    viewModel.setFilter(MangaViewModel.ChapterFilter.ALL)
                    viewModel.setGroup(null)
                },
                label = { Text(stringResource(Res.string.manga_filter_all)) },
            )
        }
        item {
            androidx.compose.material3.FilterChip(
                selected = state.filter == MangaViewModel.ChapterFilter.UNREAD,
                onClick = {
                    viewModel.setFilter(
                        if (state.filter ==
                            MangaViewModel.ChapterFilter.UNREAD
                        ) {
                            MangaViewModel.ChapterFilter.ALL
                        } else {
                            MangaViewModel.ChapterFilter.UNREAD
                        },
                    )
                },
                label = { Text(stringResource(Res.string.manga_filter_unread)) },
            )
        }
        if (state.groups.size > 1) {
            items(state.groups.size) { i ->
                val name = state.groups[i]
                androidx.compose.material3.FilterChip(
                    selected = state.group == name,
                    onClick = { viewModel.setGroup(if (state.group == name) null else name) },
                    label = { Text(name) },
                )
            }
        }
    }
}

@Composable
private fun shortDate(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return stringResource(
        Res.string.date_short,
        date.day.toString().padStart(2, '0'),
        date.month.number
            .toString()
            .padStart(2, '0'),
        date.year.toString(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Synopsis(
    text: String?,
    genres: List<String>,
    alternativeTitles: List<String>,
) {
    if (text.isNullOrBlank() && genres.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (!text.isNullOrBlank()) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded && alternativeTitles.size > 2) {
                Text(
                    alternativeTitles.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (expanded) Res.string.action_less_text else Res.string.action_more_text))
            }
        }
        if (genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                genres.take(MAX_GENRES).forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(8.dp),
                                ).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    isNew: Boolean,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkPrevious: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val numberColor =
        when {
            chapter.read -> MaterialTheme.colorScheme.onSurfaceVariant
            chapter.inProgress -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.primary
        }
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                chapter.number.chapterLabel(),
                style = MaterialTheme.typography.titleLarge,
                color = numberColor,
                modifier = Modifier.widthIn(min = 34.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    chapter.source.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.manga_chapter_label, chapter.number.chapterLabel()),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (chapter.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // RF-07: the scan group is credited and opens its page.
                val groupUrl = chapter.source.groupUrl
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                chapter.source.group?.let { group ->
                    Text(
                        group,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (groupUrl != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            if (groupUrl != null) {
                                // A real button, 48 dp tall to the finger, not a tap that also opens the chapter.
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.manga_open_group)) {
                                        uriHandler.openUri(groupUrl)
                                    }
                            } else {
                                Modifier
                            },
                    )
                }
                val meta =
                    listOfNotNull(
                        chapter.source.uploadedAtEpochMs?.let { shortDate(it) },
                        if (chapter.read) stringResource(Res.string.manga_read) else null,
                    ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (isNew) {
                    // RF-33: seen at the source after the last opening of the work.
                    Text(
                        stringResource(Res.string.manga_new),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                if (chapter.removedAtSource) {
                    Text(
                        stringResource(Res.string.manga_removed_from_source),
                        style = MaterialTheme.typography.labelSmall,
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(6.dp),
                                ).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            if (chapter.inProgress && chapter.pageCount != null) {
                Text(
                    stringResource(Res.string.manga_page_of, chapter.lastPage + 1, chapter.pageCount!!),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(HqrIcons.More, stringResource(Res.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (chapter.read) Res.string.manga_mark_unread else Res.string.manga_mark_read)) },
                        onClick = {
                            menu = false
                            onMarkRead()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.manga_mark_previous)) },
                        onClick = {
                            menu = false
                            onMarkPrevious()
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
