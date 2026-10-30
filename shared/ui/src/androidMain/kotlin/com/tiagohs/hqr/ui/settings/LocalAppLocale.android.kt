package com.tiagohs.hqr.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var device: Locale? = null

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        if (device == null) device = Locale.getDefault()
        val locale = tag?.let(Locale::forLanguageTag) ?: device!!
        Locale.setDefault(locale)
        val changed = Configuration(configuration).apply { setLocale(locale) }
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION") // The documented way for Compose resources to read the chosen locale.
        resources.updateConfiguration(changed, resources.displayMetrics)
        return LocalConfiguration.provides(changed)
    }
}
