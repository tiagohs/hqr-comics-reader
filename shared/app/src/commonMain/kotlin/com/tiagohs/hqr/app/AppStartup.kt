package com.tiagohs.hqr.app

import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.SourceRegistry

/** What every platform does once per start, before and after the first screen. */
class AppStartup(
    private val servers: ServerManager,
    private val config: SourceConfig,
    private val registry: SourceRegistry,
    private val downloads: Downloads?,
) {
    suspend fun run() {
        // RF-17, RF-88: the saved servers and switches first, from the device; screens wait for them.
        try {
            servers.start()
            config.applySaved()
        } finally {
            registry.markReady()
        }
        // RF-63: a queue left with work (app closed, phone restarted) starts again.
        downloads?.resumePending()
        // RF-105: the remote list may take a while; nothing waits for it.
        config.refreshIfStale()
    }
}

/**
 * A ViewModel outside Compose (the iOS app, tests): no navigation entry holds its state, so it gets a fresh
 * SavedStateHandle after the screen's own [params].
 */
inline fun <reified T : androidx.lifecycle.ViewModel> org.koin.core.Koin.screenViewModel(vararg params: Any?): T =
    get {
        org.koin.core.parameter
            .parametersOf(*params, androidx.lifecycle.SavedStateHandle())
    }
