package com.tiagohs.hqr.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tiagohs.hqr.data.settings.AppPreferences
import org.koin.compose.koinInject

@Composable
actual fun rememberFolderPicker(onPicked: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val prefs = koinInject<AppPreferences>()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Read only (RF-16): the permission kept across restarts is the read one.
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                prefs.setLocalFolder(uri.toString())
            }
            onPicked(uri?.toString())
        }
    return { launcher.launch(null) }
}
