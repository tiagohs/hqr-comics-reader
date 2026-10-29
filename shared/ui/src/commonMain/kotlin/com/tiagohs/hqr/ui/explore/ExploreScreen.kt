package com.tiagohs.hqr.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.explore_sources
import com.tiagohs.hqr.ui.resources.nav_explore
import com.tiagohs.hqr.ui.theme.BalloonShape
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** SCR-EXP-001: the sources (the Feed and global search join with FEAT-012 and FEAT-026). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenSource: (String) -> Unit,
    registry: SourceRegistry = koinInject(),
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(Res.string.nav_explore), style = MaterialTheme.typography.headlineLarge) })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                Text(
                    stringResource(Res.string.explore_sources),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            items(registry.all, key = { it.id.value }) { source ->
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = {
                        if (source.languages.isNotEmpty()) {
                            Text(
                                source.languages.joinToString(" · ") { it.uppercase() },
                            )
                        }
                    },
                    leadingContent = { SourceMonogram(source) },
                    trailingContent = { Icon(HqrIcons.Expand, contentDescription = null) },
                    modifier = Modifier.clickable { onOpenSource(source.id.value) },
                )
            }
        }
    }
}

/** Two-letter monogram of a source (design.md §5), never a brand logo. */
@Composable
fun SourceMonogram(source: Source) {
    val letters =
        source.name
            .split(' ', '-')
            .filter { it.isNotBlank() }
            .let { words -> if (words.size >= 2) "${words[0][0]}${words[1][0]}" else source.name.take(2) }
            .uppercase()
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, BalloonShape),
        contentAlignment = Alignment.Center,
    ) {
        if (source.id.value == "local") {
            Icon(HqrIcons.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
        } else {
            Text(letters, style = MaterialTheme.typography.labelLarge)
        }
    }
}
