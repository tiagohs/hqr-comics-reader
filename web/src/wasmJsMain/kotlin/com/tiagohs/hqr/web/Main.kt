package com.tiagohs.hqr.web

import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.ui.di.AppVersion
import com.tiagohs.hqr.data.sources.HttpSourceListFetcher
import com.tiagohs.hqr.data.sources.SourceListFetcher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.buildHqr
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.ui.di.BuiltInSources
import com.tiagohs.hqr.ui.di.coreModule
import com.tiagohs.hqr.ui.navigation.HqrApp
import io.ktor.client.HttpClient
import kotlinx.browser.document
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.w3c.dom.Worker

private fun sqliteWorker(): Worker = js("""new Worker(new URL('sqlite-worker.js', document.baseURI), { type: 'module' })""")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            module {
                single<HqrDatabase> {
                    Room.databaseBuilder<HqrDatabase>(HqrDatabase.NAME).setDriver(WebWorkerSQLiteDriver(sqliteWorker())).buildHqr()
                }
                single<Settings> { StorageSettings() }
                single<HttpClient> { hqrHttpClient("HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)") }
                single<ImageCache> { MemoryImageCache() }
                single<FileTree> { BrowserFileTree() }
                single { LocalSource(get()) }
                single {
                    val prefs = get<AppPreferences>()
                    MangaDexSource(
                        get(),
                        contentLanguages = { prefs.contentLanguages.value },
                        contentRatings = { prefs.mangaDexRatings() },
                        reportImages = false,
                    )
                }
                single<List<Source>>(BuiltInSources) { listOf(get<MangaDexSource>(), get<LocalSource>()) }
                single<SourceListFetcher> { HttpSourceListFetcher(get()) }
                single<SecretStore> { SettingsSecretStore(get()) }
                single<Connectivity> { BrowserConnectivity() }
                single(AppVersion) { "1.0.0" }
            },
            coreModule,
        )
    }
    ComposeViewport(document.body!!) { HqrApp(systemDark = isSystemInDarkTheme()) }
}
