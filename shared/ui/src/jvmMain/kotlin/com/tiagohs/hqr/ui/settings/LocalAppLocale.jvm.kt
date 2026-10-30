package com.tiagohs.hqr.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private val local = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }
    private var device: Locale? = null

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        if (device == null) device = Locale.getDefault()
        val locale = tag?.let(Locale::forLanguageTag) ?: device!!
        Locale.setDefault(locale)
        return local.provides(locale.toLanguageTag())
    }
}
