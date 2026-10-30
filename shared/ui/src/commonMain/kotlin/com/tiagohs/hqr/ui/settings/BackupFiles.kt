package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable

/** Saving and opening the backup file through the system's own picker (Android: Storage Access Framework). */
class BackupFiles(
    val save: (bytes: ByteArray, name: String) -> Unit,
    val open: () -> Unit,
)

/** Where the platform has no picker yet (the browser, until the web milestone), [save] answers false at once. */
@Composable
expect fun rememberBackupFiles(
    onSaved: (Boolean) -> Unit,
    onOpened: (ByteArray?) -> Unit,
): BackupFiles
