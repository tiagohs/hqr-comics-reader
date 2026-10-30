package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

/** RF-114: the web app shows what only the phone app does, with the reason, instead of hiding it. */
expect val runsInBrowser: Boolean

/** [runsInBrowser] for the screens, so a test can show the browser's version. */
val LocalRunsInBrowser = staticCompositionLocalOf { runsInBrowser }
