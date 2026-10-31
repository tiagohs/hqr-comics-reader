package com.tiagohs.hqr.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private fun prefersReducedMotion(): Boolean = js("""window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches""")

/** The browser's "prefers-reduced-motion", which follows the system setting. */
@Composable
actual fun rememberSystemReduceMotion(): Boolean = remember { prefersReducedMotion() }
