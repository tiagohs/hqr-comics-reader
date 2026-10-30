package com.tiagohs.hqr.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** RF-76: the system's colors (Android 12 or later); null where the platform has none. */
@Composable
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?

/** Whether this device offers the system's colors, so Settings only shows the option where it works. */
expect val dynamicColorAvailable: Boolean
