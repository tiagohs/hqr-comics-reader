package com.tiagohs.hqr.ui.manga

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.download_state_queued
import com.tiagohs.hqr.ui.resources.manga_downloaded
import com.tiagohs.hqr.ui.theme.HqrIcons
import org.jetbrains.compose.resources.stringResource

/** The download state of a chapter row: downloaded, in the queue with its pages, or nothing. */
@Composable
internal fun DownloadMark(download: DownloadItem?) {
    when (download?.state) {
        null -> Unit
        DownloadState.DONE ->
            Icon(
                HqrIcons.Downloaded,
                stringResource(Res.string.manga_downloaded),
                tint = MaterialTheme.colorScheme.primary,
            )
        DownloadState.RUNNING ->
            CircularProgressIndicator(
                progress = { if (download.pagesTotal == 0) 0f else download.pagesDone.toFloat() / download.pagesTotal },
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        else -> Icon(HqrIcons.Download, stringResource(Res.string.download_state_queued), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
