package com.tiagohs.hqr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.sources.local.ImportedFiles
import kotlinx.coroutines.delay
import org.koin.compose.getKoin

private const val SHOW_MS = 5_000L

/** RF-110: files dropped on the window say, for a few seconds, what came in and what was refused (CBR). */
@Composable
fun DroppedFilesBanner() {
    val files = getKoin().getOrNull<ImportedFiles>() ?: return
    var last by remember { mutableStateOf<ImportResult?>(null) }
    LaunchedEffect(files) { files.dropped.collect { last = it } }
    LaunchedEffect(last) {
        if (last != null) {
            delay(SHOW_MS)
            last = null
        }
    }
    val text = importMessage(last) ?: return
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
