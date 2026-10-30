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
@Suppress("TooManyFunctions") // One getter and one setter per preference, nothing else.
class AppPreferences(
    private val settings: Settings,
) {
    private val _theme = MutableStateFlow(ThemeMode.entries.getOrElse(settings.getInt(THEME, 0)) { ThemeMode.SYSTEM })
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _pureBlack = MutableStateFlow(settings.getBoolean(PURE_BLACK, false))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    // RF-77: null follows the device; else "pt", "en" or "es".
    private val _appLanguage = MutableStateFlow(settings.getStringOrNull(APP_LANGUAGE))
    val appLanguage: StateFlow<String?> = _appLanguage.asStateFlow()

    fun setAppLanguage(tag: String?) {
        if (tag == null) settings.remove(APP_LANGUAGE) else settings.putString(APP_LANGUAGE, tag)
        _appLanguage.value = tag
    }

    private val _dynamicColor = MutableStateFlow(settings.getBoolean(DYNAMIC_COLOR, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setDynamicColor(on: Boolean) {
        settings.putBoolean(DYNAMIC_COLOR, on)
        _dynamicColor.value = on
    }

    private val _crashReports = MutableStateFlow(settings.getBoolean(CRASH_REPORTS, true))
    val crashReports: StateFlow<Boolean> = _crashReports.asStateFlow()

    fun setCrashReports(on: Boolean) {
        settings.putBoolean(CRASH_REPORTS, on)
        _crashReports.value = on
    }

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

    // RF-66: off by default.
    private val _deleteAfterRead = MutableStateFlow(settings.getBoolean(DELETE_AFTER_READ, false))
    val deleteAfterRead: StateFlow<Boolean> = _deleteAfterRead.asStateFlow()

    fun setDeleteAfterRead(on: Boolean) {
        settings.putBoolean(DELETE_AFTER_READ, on)
        _deleteAfterRead.value = on
    }

    private val _mangaDexSuggestive = MutableStateFlow(settings.getBoolean(MANGADEX_SUGGESTIVE, false))

    /** RF-08: "suggestive" on MangaDex only after the 18+ confirmation; off by default. */
    val mangaDexSuggestive: StateFlow<Boolean> = _mangaDexSuggestive.asStateFlow()

    fun setMangaDexSuggestive(on: Boolean) {
        settings.putBoolean(MANGADEX_SUGGESTIVE, on)
        _mangaDexSuggestive.value = on
    }

    // RF-06: MangaDex's own chapter languages; null follows the content languages (RF-81).
    private val mangaDexLanguagesValue =
        MutableStateFlow(
            settings
                .getStringOrNull(MANGADEX_LANGUAGES)
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?.ifEmpty { null },
        )
    val mangaDexLanguagesOverride: StateFlow<Set<String>?> = mangaDexLanguagesValue.asStateFlow()

    /** The chapter languages MangaDex asks for (RF-06). */
    fun mangaDexLanguages(): Set<String> = mangaDexLanguagesValue.value ?: _contentLanguages.value

    fun setMangaDexLanguages(languages: Set<String>?) {
        val value = languages?.ifEmpty { null }
        if (value == null) settings.remove(MANGADEX_LANGUAGES) else settings.putString(MANGADEX_LANGUAGES, value.joinToString(","))
        mangaDexLanguagesValue.value = value
    }

    // RF-06: "data-saver" pages, smaller and compressed; off by default.
    private val _mangaDexDataSaver = MutableStateFlow(settings.getBoolean(MANGADEX_DATA_SAVER, false))
    val mangaDexDataSaver: StateFlow<Boolean> = _mangaDexDataSaver.asStateFlow()

    fun setMangaDexDataSaver(on: Boolean) {
        settings.putBoolean(MANGADEX_DATA_SAVER, on)
        _mangaDexDataSaver.value = on
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

    companion object {
        const val THEME = "theme"
        const val APP_LANGUAGE = "app_language"
        const val PURE_BLACK = "pure_black"
        const val DYNAMIC_COLOR = "dynamic_color"

        /** Read by the Android app before Koin starts (Crashlytics, RNF-10). */
        const val CRASH_REPORTS = "crash_reports"
        const val COVER_COLOR = "cover_color"
        const val LOCAL_FOLDER = "local_folder"
        const val ONBOARDED = "onboarded"
        const val LANGUAGES = "content_languages"
        const val DELETE_AFTER_READ = "downloads.deleteAfterRead"
        const val WIFI_ONLY = "downloads_wifi_only"
        const val MANGADEX_SUGGESTIVE = "mangadex_mangaDexSuggestive"
        const val MANGADEX_LANGUAGES = "mangadex_languages"
        const val MANGADEX_DATA_SAVER = "mangadex_data_saver"
    }
}
