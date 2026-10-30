package com.tiagohs.hqr.android

import android.app.Application
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.androidDatabase
import com.tiagohs.hqr.data.net.DiskImageCache
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.sources.HttpSourceListFetcher
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.sources.hqnow.HqNowSource
import com.tiagohs.hqr.sources.local.AndroidPdfPages
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.local.SafFileTree
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.ui.di.AppVersion
import com.tiagohs.hqr.ui.di.BuiltInSources
import com.tiagohs.hqr.ui.di.coreModule
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class HqrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        startKoin {
            androidContext(this@HqrApplication)
            modules(platformModule(), coreModule)
        }
    }

    private fun platformModule() =
        module {
            single<HqrDatabase> { androidDatabase(this@HqrApplication) }
            single<Settings> { SharedPreferencesSettings(getSharedPreferences("hqr", MODE_PRIVATE)) }
            single<HttpClient> { hqrHttpClient(userAgent()) }
            single<ImageCache> { DiskImageCache(FileSystem.SYSTEM, cacheDir.toOkioPath() / "images") }
            single { SafFileTree(contentResolver) { get<AppPreferences>().localFolder.value } }
            single<FileTree> { get<SafFileTree>() }
            single {
                LocalSource(get(), AndroidPdfPages(contentResolver, get()) { get<AppPreferences>().localFolder.value })
            }
            single {
                MangaDexSource(get(), contentLanguages = {
                    get<AppPreferences>().contentLanguages.value
                }, contentRatings = { get<AppPreferences>().mangaDexRatings() })
            }
            // RF-12: HQ Now only on Android; the browser needs the bridge for its images (FEAT-051).
            single { HqNowSource(get(), apiUrl = { get<SourceConfig>().baseUrl(SourceId.HQ_NOW) ?: HqNowSource.API }) }
            single<SourceListFetcher> { HttpSourceListFetcher(get()) }
            single<SecretStore> { KeystoreSecretStore(this@HqrApplication) }
            single(AppVersion) { BuildConfig.VERSION_NAME }
            single<List<Source>>(BuiltInSources) { listOf(get<MangaDexSource>(), get<HqNowSource>(), get<LocalSource>()) }
        }

    /** RF-10: HQR/<version> (<site>). */
    private fun userAgent(): String = "HQR/${BuildConfig.VERSION_NAME} (https://github.com/tiagohs/hqr-comics-reader)"
}
