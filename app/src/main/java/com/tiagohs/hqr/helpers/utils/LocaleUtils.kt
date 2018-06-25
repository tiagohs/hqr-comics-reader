package com.tiagohs.hqr.helpers.utils

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.getResourceDrawable
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import java.util.*

class LocaleUtils(
        private val preferences: PreferenceHelper
) {

    private var systemLocale: Locale? = null

    private var appLocale = getLocaleFromString(preferences.laguage())

    private var currentLocale: Locale? = null

    fun getLocaleFromString(pref: String): Locale? {
        if (pref.isNullOrEmpty()) {
            return null
        }
        return getLocale(pref)
    }

    fun getLocaleImage(lang: String?, context: Context?): Drawable? {
        return when(lang) {
            "pt-BR" -> context?.getResourceDrawable(R.drawable.ic_brazil)
            "en" -> context?.getResourceDrawable(R.drawable.ic_eua)
            "es" -> context?.getResourceDrawable(R.drawable.ic_spain)
            else -> null
        }
    }

    fun getDisplayName(lang: String?, context: Context): String {
        return when (lang) {
            null -> ""
            "" -> context.getString(R.string.other_source)
            "all" -> context.getString(R.string.all_lang)
            else -> {
                val locale = getLocale(lang)
                locale.getDisplayName(locale).capitalize()
            }
        }
    }

    private fun getLocale(lang: String): Locale {
        val sp = lang.split("_", "-")
        return when (sp.size) {
            2 -> Locale(sp[0], sp[1])
            3 -> Locale(sp[0], sp[1], sp[2])
            else -> Locale(lang)
        }
    }

    fun changeLocale(pref: String) {
        appLocale = getLocaleFromString(pref)
    }

    fun updateConfiguration(wrapper: ContextThemeWrapper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && appLocale != null) {
            val config = Configuration(preferences.context.resources.configuration)
            config.setLocale(appLocale)
            wrapper.applyOverrideConfiguration(config)
        }
    }

    fun updateConfiguration(app: Application, config: Configuration, configChange: Boolean = false) {
        if (systemLocale == null) {
            systemLocale = getConfigLocale(config)
        }
        if (configChange) {
            val configLocale = getConfigLocale(config)
            if (currentLocale == configLocale) {
                return
            }
            systemLocale = configLocale
        }
        currentLocale = appLocale ?: systemLocale ?: Locale.getDefault()
        val newConfig = updateConfigLocale(config, currentLocale!!)
        val resources = app.resources
        resources.updateConfiguration(newConfig, resources.displayMetrics)

        Locale.setDefault(currentLocale)
    }

    private fun getConfigLocale(config: Configuration): Locale {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            config.locale
        } else {
            config.locales[0]
        }
    }

    private fun updateConfigLocale(config: Configuration, locale: Locale): Configuration {
        val newConfig = Configuration(config)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            newConfig.locale = locale
        } else {
            newConfig.locales = LocaleList(locale)
        }
        return newConfig
    }
}