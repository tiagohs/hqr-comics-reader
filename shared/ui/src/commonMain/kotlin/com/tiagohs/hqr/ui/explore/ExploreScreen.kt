package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.ui.components.sourceDisplayName
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.bridge_needed
import com.tiagohs.hqr.ui.resources.explore_manage_sources
import com.tiagohs.hqr.ui.resources.explore_search_all
import com.tiagohs.hqr.ui.resources.explore_sources
import com.tiagohs.hqr.ui.resources.feed_tab
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.resources.search_hint
import com.tiagohs.hqr.ui.resources.sources_tab
import com.tiagohs.hqr.ui.theme.BalloonShape
import com.tiagohs.hqr.ui.theme.HqrIcons
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
    // RF-25: the sources and the feed; the sources first, as they lead everywhere else.
    var tab by rememberSaveable { mutableStateOf(0) }
    // RF-03: only the sources that are on.
    val active by registry.active.collectAsStateWithLifecycle()
    // RF-108: in the browser, online sources stay listed and say they need the bridge.
    val bridge = getKoin().getOrNull<Bridge>()
    // The bridge exists or not for the whole run: the condition never changes between compositions.
    val bridgeAddress = bridge?.address?.collectAsStateWithLifecycle()?.value
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.nav_explore), style = MaterialTheme.typography.headlineLarge) },
            actions = { IconButton(onClick = onSearch) { Icon(HqrIcons.Search, stringResource(Res.string.explore_search_all)) } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(Res.string.sources_tab)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(Res.string.feed_tab)) })
            }
            if (tab == 1) {
                FeedTab(onOpenManga = onOpenManga)
            } else {
                SourcesList(active, bridge, bridgeAddress, onSearch, onOpenSource, onBridge, onManageSources)
            }
        }
    }
}

@Composable
private fun SourcesList(
    active: List<Source>,
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
            item {
                Text(
                    stringResource(Res.string.explore_sources),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            items(active, key = { it.id.value }) { source ->
                val needsBridge = bridge != null && bridgeAddress == null && bridge.needs(source.id)
                ListItem(
                    headlineContent = { Text(sourceDisplayName(source.id, source.name)) },
                    supportingContent = {
                        if (needsBridge) {
                            Text(stringResource(Res.string.bridge_needed), color = MaterialTheme.colorScheme.error)
                        } else if (source.languages.isNotEmpty()) {
                            Text(
                                source.languages.joinToString(" · ") { it.uppercase() },
                            )
                        }
                    },
                    leadingContent = { SourceMonogram(source) },
                    trailingContent = { Icon(HqrIcons.Expand, contentDescription = null) },
                    modifier = Modifier.clickable { if (needsBridge) onBridge() else onOpenSource(source.id.value) },
                )
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

/** Two-letter monogram of a source (design.md §5), never a brand logo. */
@Composable
fun SourceMonogram(source: Source) = SourceMonogram(source.name, isLocal = source.id.value == "local")

@Composable
fun SourceMonogram(
    name: String,
    isLocal: Boolean = false,
) {
    val letters =
        name
            .split(' ', '-')
            .filter { it.isNotBlank() }
            .let { words -> if (words.size >= 2) "${words[0][0]}${words[1][0]}" else name.take(2) }
            .uppercase()
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, BalloonShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isLocal) {
            Icon(HqrIcons.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
        } else {
            Text(letters, style = MaterialTheme.typography.labelLarge)
        }
    }
}
