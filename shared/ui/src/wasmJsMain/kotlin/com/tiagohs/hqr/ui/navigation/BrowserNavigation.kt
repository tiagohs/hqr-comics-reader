package com.tiagohs.hqr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavHostController
import androidx.navigation.bindToBrowserNavigation

/**
 * The route becomes the address fragment (`#…`); reloading opens the same screen. The reader opens with "continue"
 * by default, so a reload goes back to the page saved half a second after the last turn (RF-43).
 */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
actual fun BrowserNavigation(nav: NavHostController) {
    LaunchedEffect(nav) { nav.bindToBrowserNavigation() }
}
