package com.tiagohs.hqr.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        // "Seguir o aparelho" reads the device's language every time, so a change made with the app open is followed.
        val locale = tag?.let(Locale::forLanguageTag) ?: deviceLocale(context)
        // Compose resources read the default locale (the documented way to switch the language inside the app).
        Locale.setDefault(locale)
        val changed = Configuration(configuration).apply { setLocale(locale) }
        val resources = context.resources
        @Suppress("DEPRECATION") // The documented way for Compose resources to read the chosen locale.
        resources.updateConfiguration(changed, resources.displayMetrics)
        return LocalConfiguration.provides(changed)
    }

    /** The language the user gave this app in Android's settings (13+), or the device's. */
    private fun deviceLocale(context: Context): Locale {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perApp = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (perApp != null && !perApp.isEmpty) return perApp[0]
        }
        return Resources.getSystem().configuration.locales[0]
    }
}
