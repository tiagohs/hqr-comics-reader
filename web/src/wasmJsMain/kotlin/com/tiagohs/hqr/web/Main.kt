package com.tiagohs.hqr.web

import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.app.AppVersion
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
import com.tiagohs.hqr.data.net.httpClient
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.ImportedFiles
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.app.BuiltInSources
import com.tiagohs.hqr.app.coreModule
import com.tiagohs.hqr.ui.di.uiModule
import com.tiagohs.hqr.ui.navigation.HqrApp
import io.ktor.client.HttpClient
import com.tiagohs.hqr.data.bridge.PersonalBridge
import com.tiagohs.hqr.data.bridge.routeThroughBridge
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.sources.hqnow.HqNowSource
import com.tiagohs.hqr.sources.archive.InternetArchiveSource
import com.tiagohs.hqr.sources.mangaflix.MangaFlixSource
import com.tiagohs.hqr.sources.namicomi.NamiComiSource
import com.tiagohs.hqr.sources.weebcentral.WeebCentralSource
import com.tiagohs.hqr.sources.xoxo.XoxoSource
import com.tiagohs.hqr.sources.yomu.YomuSource
import kotlinx.browser.document
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.w3c.dom.Worker

private const val USER_AGENT = "HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)"

private fun sqliteWorker(): Worker = js("""new Worker(new URL('sqlite-worker.js', document.baseURI), { type: 'module' })""")

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("LongMethod") // One registration per line: the web app's whole wiring in one place.
fun main() {
    startKoin {
        modules(
            module {
                single<HqrDatabase> {
                    Room.databaseBuilder<HqrDatabase>(HqrDatabase.NAME).setDriver(WebWorkerSQLiteDriver(sqliteWorker())).buildHqr()
                }
                single<Settings> { StorageSettings() }
                // RF-108, ADR-023: requests for the online sources go through the user's bridge; the bridge's own test
                // goes straight to it, with no retries ("Testar" answers at once).
                single { PersonalBridge(get(), get(), httpClient()) }
                single<Bridge> { get<PersonalBridge>() }
                single<HttpClient> { hqrHttpClient(USER_AGENT) { routeThroughBridge(get()) } }
                single<ImageCache> { MemoryImageCache() }
                single { BrowserFileTree() }
                single<FileTree> { get<BrowserFileTree>() }
                single<ImportedFiles> { get<BrowserFileTree>() }
                single { LocalSource(get(), get<BrowserFileTree>()) }
                single {
                    val prefs = get<AppPreferences>()
                    MangaDexSource(
                        get(),
                        contentLanguages = { prefs.mangaDexLanguages() },
                        contentRatings = { prefs.mangaDexRatings() },
                        dataSaver = { prefs.mangaDexDataSaver.value },
                        reportImages = false,
                    )
                }
                single { HqNowSource(get(), apiUrl = { get<SourceConfig>().baseUrl(SourceId.HQ_NOW) ?: HqNowSource.API }) }
                // RF-99 to RF-104: the sources added in 1.1.
                single { InternetArchiveSource(get()) }
                single {
                    val prefs = get<AppPreferences>()
                    NamiComiSource(
                        get(),
                        contentLanguages = { prefs.contentLanguages.value },
                        // RF-08: "restricted" only with suggestive content on; "mature" never.
                        contentRatings = { if (prefs.mangaDexSuggestive.value) setOf("safe", "restricted") else setOf("safe") },
                    )
                }
                single { MangaFlixSource(get(), apiUrl = { get<SourceConfig>().baseUrl(MangaFlixSource.ID) ?: MangaFlixSource.API }) }
                single { YomuSource(get(), apiUrl = { get<SourceConfig>().baseUrl(YomuSource.ID) ?: YomuSource.API }) }
                single { WeebCentralSource(get()) }
                single { XoxoSource(get()) }
                single<List<Source>>(BuiltInSources) {
                    listOf(
                            get<MangaDexSource>(),
                            get<HqNowSource>(),
                            get<InternetArchiveSource>(),
                            get<NamiComiSource>(),
                            get<MangaFlixSource>(),
                            get<YomuSource>(),
                            get<WeebCentralSource>(),
                            get<XoxoSource>(),
                            get<LocalSource>(),
                        )
                }
                single<SourceListFetcher> { HttpSourceListFetcher(get()) }
                single<SecretStore> { SettingsSecretStore(get()) }
                single<Connectivity> { BrowserConnectivity() }
                single(AppVersion) { "1.0.0" }
            },
            coreModule,
            uiModule,
        )
    }
    ComposeViewport(document.body!!) { HqrApp(systemDark = isSystemInDarkTheme()) }
}
