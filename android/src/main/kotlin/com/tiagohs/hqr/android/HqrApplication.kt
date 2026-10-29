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
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.sources.local.AndroidPdfPages
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.local.SafFileTree
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
            single<List<Source>>(BuiltInSources) { listOf(get<LocalSource>()) }
        }

    /** RF-10: HQR/<version> (<site>). */
    private fun userAgent(): String = "HQR/${BuildConfig.VERSION_NAME} (https://github.com/tiagohs/hqr-comics-reader)"
}
