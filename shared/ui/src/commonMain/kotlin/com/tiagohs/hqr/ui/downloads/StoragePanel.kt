package com.tiagohs.hqr.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.storage_cache
import com.tiagohs.hqr.ui.resources.storage_cache_note
import com.tiagohs.hqr.ui.resources.storage_clear_cache
import com.tiagohs.hqr.ui.resources.storage_downloads
import com.tiagohs.hqr.ui.resources.storage_title
import com.tiagohs.hqr.ui.resources.storage_used
import com.tiagohs.hqr.ui.theme.bangers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin

/**
 * RF-66, RF-80: the space of the downloads and of the image cache next to what is free, as one bar with a legend, and
 * the way to clear the cache (the downloads stay).
 */
@Composable
internal fun StoragePanel(
    downloadsBytes: Long,
    chapters: Int,
) {
    val koin = getKoin()
    val cache = remember(koin) { koin.getOrNull<ImageCache>() }
    val free = remember(koin) { koin.getOrNull<FreeSpace>() }?.bytes()
    var cacheBytes by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(cache) { cacheBytes = cache?.size() ?: 0L }
    val ink = MaterialTheme.colorScheme.onSurface
    val downloadsColor = MaterialTheme.colorScheme.primary
    val total = (downloadsBytes + cacheBytes + (free ?: 0L)).coerceAtLeast(1L)
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(Res.string.storage_title), style = MaterialTheme.typography.headlineSmall)
        Column(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        ink,
                        topLeft =
                            androidx.compose.ui.geometry
                                .Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = size,
                    )
                }.background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(3.dp, ink)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatBytes(downloadsBytes + cacheBytes), fontFamily = bangers(), fontSize = 36.sp)
                free?.let {
                    Text(
                        stringResource(Res.string.storage_used, formatBytes(it)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().height(20.dp).border(2.dp, ink)) {
                val parts = listOf(downloadsBytes to downloadsColor, cacheBytes to ink)
                parts.forEach { (bytes, color) ->
                    if (bytes > 0) Box(Modifier.weight(bytes.toFloat() / total).fillMaxHeight().background(color))
                }
                val rest = (total - downloadsBytes - cacheBytes).coerceAtLeast(0L)
                if (rest > 0) Box(Modifier.weight(rest.toFloat() / total).fillMaxHeight())
            }
            Legend(downloadsColor, pluralFree(Res.string.storage_downloads, chapters), formatBytes(downloadsBytes))
            Legend(ink, stringResource(Res.string.storage_cache), formatBytes(cacheBytes))
            if (cache != null) {
                OutlinedButton(onClick = {
                    scope.launch {
                        cache.clear()
                        cacheBytes = cache.size()
                    }
                }) { Text(stringResource(Res.string.storage_clear_cache)) }
                Text(
                    stringResource(Res.string.storage_cache_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun pluralFree(
    res: org.jetbrains.compose.resources.StringResource,
    count: Int,
): String = stringResource(res, count)

@Composable
private fun Legend(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(14.dp).background(color).border(2.dp, MaterialTheme.colorScheme.onSurface))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, fontFamily = bangers(), fontSize = 17.sp)
    }
}
