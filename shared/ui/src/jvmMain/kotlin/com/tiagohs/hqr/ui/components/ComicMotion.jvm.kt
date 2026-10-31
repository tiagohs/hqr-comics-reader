package com.tiagohs.hqr.ui.components

import androidx.compose.runtime.Composable

/** The desktop build only runs the screen tests: full motion. */
@Composable
actual fun rememberSystemReduceMotion(): Boolean = false
