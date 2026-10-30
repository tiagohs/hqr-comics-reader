package com.tiagohs.hqr.ui.downloads

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDownloadFolderPicker(onPicked: (String?) -> Unit): () -> Unit = { onPicked(null) }
