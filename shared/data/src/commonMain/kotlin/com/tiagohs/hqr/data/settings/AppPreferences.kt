package com.tiagohs.hqr.data.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * User preferences (RF-76, RF-77, RF-81, RF-98). Stored with multiplatform-settings: SharedPreferences on Android,
 * localStorage in the browser (ADR-004 keeps DataStore's role; this store also runs on the web).
 */
class AppPreferences(
    private val settings: Settings,
) {
    private val _theme = MutableStateFlow(ThemeMode.entries.getOrElse(settings.getInt(THEME, 0)) { ThemeMode.SYSTEM })
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _pureBlack = MutableStateFlow(settings.getBoolean(PURE_BLACK, false))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _coverColor = MutableStateFlow(settings.getBoolean(COVER_COLOR, true))
    val coverColor: StateFlow<Boolean> = _coverColor.asStateFlow()

    private val _localFolder = MutableStateFlow(settings.getStringOrNull(LOCAL_FOLDER))
    val localFolder: StateFlow<String?> = _localFolder.asStateFlow()

    private val _onboarded = MutableStateFlow(settings.getBoolean(ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _contentLanguages =
        MutableStateFlow(
            settings
                .getString(LANGUAGES, "pt-br")
                .split(',')
                .filter { it.isNotBlank() }
                .toSet(),
        )
    val contentLanguages: StateFlow<Set<String>> = _contentLanguages.asStateFlow()

    private val _wifiOnlyDownloads = MutableStateFlow(settings.getBoolean(WIFI_ONLY, true))
    val wifiOnlyDownloads: StateFlow<Boolean> = _wifiOnlyDownloads.asStateFlow()

    private val _mangaDexSuggestive = MutableStateFlow(settings.getBoolean(MANGADEX_SUGGESTIVE, false))

    /** RF-08: "suggestive" on MangaDex only after the 18+ confirmation; off by default. */
    val mangaDexSuggestive: StateFlow<Boolean> = _mangaDexSuggestive.asStateFlow()

    fun setMangaDexSuggestive(on: Boolean) {
        settings.putBoolean(MANGADEX_SUGGESTIVE, on)
        _mangaDexSuggestive.value = on
    }

    /** The MangaDex content ratings to ask for (RF-08): never erotica or pornographic. */
    fun mangaDexRatings(): Set<String> = if (_mangaDexSuggestive.value) setOf("safe", "suggestive") else setOf("safe")

    fun setTheme(mode: ThemeMode) {
        settings.putInt(THEME, mode.ordinal)
        _theme.value = mode
    }

    fun setPureBlack(on: Boolean) {
        settings.putBoolean(PURE_BLACK, on)
        _pureBlack.value = on
    }

    fun setCoverColor(on: Boolean) {
        settings.putBoolean(COVER_COLOR, on)
        _coverColor.value = on
    }

    fun setLocalFolder(uri: String?) {
        if (uri == null) settings.remove(LOCAL_FOLDER) else settings.putString(LOCAL_FOLDER, uri)
        _localFolder.value = uri
    }

    fun setOnboarded() {
        settings.putBoolean(ONBOARDED, true)
        _onboarded.value = true
    }

    fun setContentLanguages(languages: Set<String>) {
        settings.putString(LANGUAGES, languages.joinToString(","))
        _contentLanguages.value = languages
    }

    fun setWifiOnlyDownloads(on: Boolean) {
        settings.putBoolean(WIFI_ONLY, on)
        _wifiOnlyDownloads.value = on
    }

    fun getString(key: String): String? = settings.getStringOrNull(key)

    fun putString(
        key: String,
        value: String?,
    ) = if (value == null) settings.remove(key) else settings.putString(key, value)

    fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = settings.getBoolean(key, default)

    fun putBoolean(
        key: String,
        value: Boolean,
    ) = settings.putBoolean(key, value)

    private companion object {
        const val THEME = "theme"
        const val PURE_BLACK = "pure_black"
        const val COVER_COLOR = "cover_color"
        const val LOCAL_FOLDER = "local_folder"
        const val ONBOARDED = "onboarded"
        const val LANGUAGES = "content_languages"
        const val WIFI_ONLY = "downloads_wifi_only"
        const val MANGADEX_SUGGESTIVE = "mangadex_mangaDexSuggestive"
    }
}
