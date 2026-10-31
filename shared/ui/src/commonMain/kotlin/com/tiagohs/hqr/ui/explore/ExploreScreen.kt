package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.feed.FavoriteSources
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.bridge_needed
import com.tiagohs.hqr.ui.resources.explore_favorites
import com.tiagohs.hqr.ui.resources.explore_favorites_hint
import com.tiagohs.hqr.ui.resources.explore_manage_sources
import com.tiagohs.hqr.ui.resources.explore_other_sources
import com.tiagohs.hqr.ui.resources.explore_search_all
import com.tiagohs.hqr.ui.resources.explore_sources
import com.tiagohs.hqr.ui.resources.feed_full_favorite
import com.tiagohs.hqr.ui.resources.feed_tab
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.search_hint
import com.tiagohs.hqr.ui.resources.source_favorite
import com.tiagohs.hqr.ui.resources.source_unfavorite
import com.tiagohs.hqr.ui.resources.sources_tab
import com.tiagohs.hqr.ui.theme.HqrIcons
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/** SCR-EXP-001: global search on top, then the sources and the feed (RF-03, RF-25). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenSource: (String) -> Unit,
    onSearch: () -> Unit = {},
    onManageSources: () -> Unit = {},
    onBridge: () -> Unit = {},
    onOpenManga: (Long) -> Unit = {},
    registry: SourceRegistry = koinInject(),
) {
    // RF-25: the feed is the first tab and opens when it has something; empty, the sources open.
    val searches = getKoin().getOrNull<SearchRepository>()
    val feedSize by remember(searches) { searches?.observeFeed()?.map { it.size } ?: flowOf(0) }.collectAsStateWithLifecycle(-1)
    var chosen by rememberSaveable { mutableStateOf(-1) }
    val tab =
        if (chosen >= 0) {
            chosen
        } else if (feedSize > 0) {
            FEED
        } else {
            SOURCES
        }
    // RF-03: only the sources that are on.
    val active by registry.active.collectAsStateWithLifecycle()
    // RF-108: in the browser, online sources stay listed and say they need the bridge.
    val bridge = getKoin().getOrNull<Bridge>()
    // The bridge exists or not for the whole run: the condition never changes between compositions.
    val bridgeAddress = bridge?.address?.collectAsStateWithLifecycle()?.value
    val favoriteSources = getKoin().getOrNull<FavoriteSources>()
    val favorites by remember(favoriteSources) { favoriteSources?.observe() ?: flowOf(emptySet()) }.collectAsStateWithLifecycle(emptySet())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fullMessage = stringResource(Res.string.feed_full_favorite)
    // RF-115: the star puts the source's latest in the feed or takes them out.
    val toggleFavorite: (Source) -> Unit = { source ->
        scope.launch {
            if (favoriteSources?.toggle(source.id) == FavoriteSources.Result.FEED_FULL) snackbar.showSnackbar(fullMessage)
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.nav_explore), style = MaterialTheme.typography.headlineLarge) },
            actions = { IconButton(onClick = onSearch) { Icon(HqrIcons.Search, stringResource(Res.string.explore_search_all)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == FEED, onClick = { chosen = FEED }, text = { Text(stringResource(Res.string.feed_tab)) })
                Tab(selected = tab == SOURCES, onClick = { chosen = SOURCES }, text = { Text(stringResource(Res.string.sources_tab)) })
            }
            if (tab == FEED) {
                FeedTab(onOpenManga = onOpenManga)
            } else {
                SourcesList(active, favorites, toggleFavorite, bridge, bridgeAddress, onSearch, onOpenSource, onBridge, onManageSources)
            }
        }
    }
}

@Composable
private fun SourcesList(
    active: List<Source>,
    favorites: Set<SourceId>,
    onToggleFavorite: (Source) -> Unit,
    bridge: Bridge?,
    bridgeAddress: String?,
    onSearch: () -> Unit,
    onOpenSource: (String) -> Unit,
    onBridge: () -> Unit,
    onManageSources: () -> Unit,
) {
    run {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                // RF-23: the way into global search, shaped like the field it opens.
                Surface(
                    onClick = onSearch,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HqrIcons.Search, contentDescription = null)
                        Text(
                            stringResource(Res.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            // RF-115: favorites first; they are the ones in the feed.
            val (favored, others) = active.partition { it.id in favorites }
            if (favored.isNotEmpty()) {
                item { SectionTitle(stringResource(Res.string.explore_favorites), stringResource(Res.string.explore_favorites_hint)) }
                items(favored, key = { "f" + it.id.value }) { source ->
                    SourceRow(source, true, bridge, bridgeAddress, onOpenSource, onBridge, onToggleFavorite)
                }
            }
            item {
                SectionTitle(stringResource(if (favored.isEmpty()) Res.string.explore_sources else Res.string.explore_other_sources), null)
            }
            items(others, key = { it.id.value }) { source ->
                SourceRow(source, false, bridge, bridgeAddress, onOpenSource, onBridge, onToggleFavorite)
            }
            // SCR-SRC-001: switches, the local folder and the servers.
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.explore_manage_sources)) },
                    leadingContent = { Icon(HqrIcons.Settings, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onManageSources),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    hint: String?,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun SourceRow(
    source: Source,
    favorite: Boolean,
    bridge: Bridge?,
    bridgeAddress: String?,
    onOpenSource: (String) -> Unit,
    onBridge: () -> Unit,
    onToggleFavorite: (Source) -> Unit,
) {
    val needsBridge = bridge != null && bridgeAddress == null && bridge.needs(source.id)
    val name = sourceDisplayName(source.id, source.name)
    ListItem(
        headlineContent = { Text(name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp)) },
        supportingContent = {
            if (needsBridge) {
                Text(stringResource(Res.string.bridge_needed), color = MaterialTheme.colorScheme.error)
            } else if (source.languages.isNotEmpty()) {
                Text(source.languages.joinToString(" · ") { it.uppercase() })
            }
        },
        leadingContent = { SourceMonogram(source, favorite) },
        trailingContent = {
            IconButton(onClick = { onToggleFavorite(source) }) {
                Icon(
                    if (favorite) HqrIcons.Star else HqrIcons.StarOutline,
                    stringResource(if (favorite) Res.string.source_unfavorite else Res.string.source_favorite, name),
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { if (needsBridge) onBridge() else onOpenSource(source.id.value) },
    )
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
}

/** Two-letter monogram of a source (design.md §5), never a brand logo. */
@Composable
fun SourceMonogram(
    source: Source,
    favorite: Boolean = false,
) = SourceMonogram(source.name, isLocal = source.id.value == "local", favorite = favorite)

@Composable
fun SourceMonogram(
    name: String,
    isLocal: Boolean = false,
    favorite: Boolean = false,
) {
    val letters =
        name
            .split(' ', '-')
            .filter { it.isNotBlank() }
            .let { words -> if (words.size >= 2) "${words[0][0]}${words[1][0]}" else name.take(2) }
            .uppercase()
    // ADR-026: a small ink-framed panel; the cover color fills it for a favorite.
    val fill = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest
    val text = if (favorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier.size(44.dp).background(fill).border(2.dp, MaterialTheme.colorScheme.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (isLocal) {
            Icon(HqrIcons.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = text)
        } else {
            Text(letters, fontFamily = bangers(), fontSize = 20.sp, color = text)
        }
    }
}

private const val FEED = 0
private const val SOURCES = 1
