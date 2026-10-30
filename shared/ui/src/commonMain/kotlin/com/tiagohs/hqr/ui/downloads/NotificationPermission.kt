package com.tiagohs.hqr.ui.downloads

import androidx.compose.runtime.Composable

/** RF-94: asks once for the notification permission where the system has one (Android 13+); elsewhere does nothing. */
@Composable
expect fun rememberNotificationPermission(): () -> Unit
