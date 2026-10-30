package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable

// In the browser the files are imported (FEAT-052, SCR-WEB-002) instead of picking a folder.
@Composable
actual fun rememberFolderPicker(onPicked: (String?) -> Unit): () -> Unit = { onPicked(null) }
