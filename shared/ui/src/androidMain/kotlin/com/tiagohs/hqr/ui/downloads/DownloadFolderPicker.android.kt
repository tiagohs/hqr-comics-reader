package com.tiagohs.hqr.ui.downloads

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDownloadFolderPicker(onPicked: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            // Kept across restarts: the queue writes there from the background (RF-67).
            // A folder whose permission can't be kept is not offered: the queue could not write there later.
            val kept =
                uri?.takeIf {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }.isSuccess
                }
            onPicked(kept?.toString())
        }
    return { launcher.launch(null) }
}
