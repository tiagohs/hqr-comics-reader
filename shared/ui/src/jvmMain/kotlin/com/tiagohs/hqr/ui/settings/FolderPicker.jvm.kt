package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable

// Desktop JVM only runs tests.
@Composable
actual fun rememberFolderPicker(onPicked: (String?) -> Unit): () -> Unit = { onPicked(null) }
