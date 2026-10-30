package com.tiagohs.hqr.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupFiles(
    onSaved: (Boolean) -> Unit,
    onOpened: (ByteArray?) -> Unit,
): BackupFiles {
    val resolver = LocalContext.current.contentResolver
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    val create =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
            val bytes = pending
            pending = null
            onSaved(
                uri != null &&
                    bytes != null &&
                    runCatching { resolver.openOutputStream(uri)!!.use { it.write(bytes) } }.isSuccess,
            )
        }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onOpened(uri?.let { runCatching { resolver.openInputStream(it)!!.use { s -> s.readBytes() } }.getOrNull() })
        }
    return remember {
        BackupFiles(
            save = { bytes, name ->
                pending = bytes
                create.launch(name)
            },
            open = { open.launch(arrayOf("application/gzip", "application/octet-stream", "*/*")) },
        )
    }
}
