package com.tiagohs.hqr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/** RF-113: binds the navigation to the browser's address and history; nothing outside the browser. */
@Composable
expect fun BrowserNavigation(nav: NavHostController)
