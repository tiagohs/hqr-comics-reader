package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// The browser's backup arrives with the web milestone (FEAT-052).
@Composable
actual fun rememberBackupFiles(
    onSaved: (Boolean) -> Unit,
    onOpened: (ByteArray?) -> Unit,
): BackupFiles = remember { BackupFiles(save = { _, _ -> onSaved(false) }, open = { onOpened(null) }) }
