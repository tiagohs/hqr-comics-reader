package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable

/**
 * Opens the system folder picker (RF-13): the Storage Access Framework on Android, the file import in the browser.
 * The chosen folder is saved in the preferences; [onPicked] receives its address or null when cancelled.
 */
@Composable
expect fun rememberFolderPicker(onPicked: (String?) -> Unit): () -> Unit
