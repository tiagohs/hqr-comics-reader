package com.tiagohs.hqr.ui.downloads

import androidx.compose.runtime.Composable

/**
 * RF-67: the system folder picker with permission to write (Android); [onPicked] receives the folder's address, or null
 * when cancelled. Where downloads don't run (browser, desktop tests), nothing is picked.
 */
@Composable
expect fun rememberDownloadFolderPicker(onPicked: (String?) -> Unit): () -> Unit
