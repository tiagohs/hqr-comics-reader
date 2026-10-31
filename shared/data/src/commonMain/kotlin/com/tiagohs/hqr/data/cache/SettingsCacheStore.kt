package com.tiagohs.hqr.data.cache

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.domain.repository.CacheStore
import com.tiagohs.hqr.domain.repository.CachedText
import kotlin.time.Clock

/**
 * [CacheStore] in the same key-value store as the preferences (SharedPreferences, localStorage, NSUserDefaults):
 * small answers only, such as the first page of a favorite source (RF-116) and when a work was last refreshed (RF-117).
 */
class SettingsCacheStore(
    private val settings: Settings,
    private val clock: Clock = Clock.System,
) : CacheStore {
    override fun read(key: String): CachedText? {
        val text = settings.getStringOrNull(PREFIX + key) ?: return null
        val at = settings.getLongOrNull(PREFIX + key + AT) ?: return null
        return CachedText(text, at)
    }

    override fun write(
        key: String,
        text: String,
    ) {
        settings.putString(PREFIX + key, text)
        settings.putLong(PREFIX + key + AT, clock.now().toEpochMilliseconds())
    }

    override fun remove(key: String) {
        settings.remove(PREFIX + key)
        settings.remove(PREFIX + key + AT)
    }

    private companion object {
        const val PREFIX = "cache."
        const val AT = ".at"
    }
}
