package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * RF-77: the language chosen inside the app ("pt", "en", "es"; null follows the device), given to the screens so the
 * string resources answer in it. The browser follows its own language.
 */
expect object LocalAppLocale {
    @Composable
    infix fun provides(tag: String?): ProvidedValue<*>
}
