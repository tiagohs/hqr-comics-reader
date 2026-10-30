package com.tiagohs.hqr.data.settings

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.domain.source.SecretStore

/**
 * Secrets in the browser's own storage for this site (architecture §3, web); Android uses the Keystore instead.
 * Kept under their own prefix, which the backup never reads (RF-72).
 */
class SettingsSecretStore(
    private val settings: Settings,
) : SecretStore {
    override suspend fun get(key: String): String? = settings.getStringOrNull(PREFIX + key)

    override suspend fun put(
        key: String,
        value: String,
    ) = settings.putString(PREFIX + key, value)

    override suspend fun remove(key: String) = settings.remove(PREFIX + key)

    private companion object {
        const val PREFIX = "secret."
    }
}
