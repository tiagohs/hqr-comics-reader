package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

// The browser keeps its own language (the choice inside the app arrives with the web milestone).
actual object LocalAppLocale {
    private val local = staticCompositionLocalOf<String?> { null }

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> = local.provides(tag)
}
