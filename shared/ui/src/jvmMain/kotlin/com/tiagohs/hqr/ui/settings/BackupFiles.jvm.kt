@file:Suppress("MatchingDeclarationName") // The actual and its test stand-in live together.

package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The JVM build only runs the screen tests: a file kept in memory stands for the picker. */
object JvmBackupFile {
    var bytes: ByteArray? = null
}

@Composable
actual fun rememberBackupFiles(
    onSaved: (Boolean) -> Unit,
    onOpened: (ByteArray?) -> Unit,
): BackupFiles =
    remember {
        BackupFiles(
            save = { bytes, _ ->
                JvmBackupFile.bytes = bytes
                onSaved(true)
            },
            open = { onOpened(JvmBackupFile.bytes) },
        )
    }
