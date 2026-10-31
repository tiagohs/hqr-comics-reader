package com.tiagohs.hqr.android

import android.app.Application
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tiagohs.hqr.app.AppVersion
import com.tiagohs.hqr.app.BuiltInSources
import com.tiagohs.hqr.app.coreModule
import com.tiagohs.hqr.app.downloadModule
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.androidDatabase
import com.tiagohs.hqr.data.download.DownloadExport
import com.tiagohs.hqr.data.download.DownloadFiles
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.net.ChallengeCookies
import com.tiagohs.hqr.data.net.DiskImageCache
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.net.useChallengeCookies
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.sources.HttpSourceListFetcher
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.data.update.ReleaseChecker
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.repository.Updates
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.sources.archive.InternetArchiveSource
import com.tiagohs.hqr.sources.hqnow.HqNowSource
import com.tiagohs.hqr.sources.local.AndroidPdfPages
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.JvmArchiveUnpacker
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.local.SafFileTree
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.sources.mangaflix.MangaFlixSource
import com.tiagohs.hqr.sources.namicomi.NamiComiSource
import com.tiagohs.hqr.sources.opds.BookStore
import com.tiagohs.hqr.sources.opds.DiskBookStore
import com.tiagohs.hqr.sources.weebcentral.WeebCentralSource
import com.tiagohs.hqr.sources.xoxo.XoxoSource
import com.tiagohs.hqr.sources.yomu.YomuSource
import com.tiagohs.hqr.ui.di.CrashReports
import com.tiagohs.hqr.ui.di.uiModule
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
            modules(platformModule(), coreModule, downloadModule, uiModule)
        }
    }

    @Suppress("LongMethod") // One registration per line: the app's whole wiring in one place.
    private fun platformModule() =
        module {
            single<HqrDatabase> { androidDatabase(this@HqrApplication) }
            single<Settings> { SharedPreferencesSettings(getSharedPreferences("hqr", MODE_PRIVATE)) }
            // RF-106: what passes a Cloudflare verification in the app's browser serves the source's requests too.
            single<ChallengeCookies> { WebViewChallengeCookies(this@HqrApplication) }
            single<HttpClient> { hqrHttpClient(userAgent()) { useChallengeCookies(get()) } }
            single<ImageCache> { DiskImageCache(FileSystem.SYSTEM, cacheDir.toOkioPath() / "images") }
            single { SafFileTree(contentResolver) { get<AppPreferences>().localFolder.value } }
            single<FileTree> { get<SafFileTree>() }
            single {
                LocalSource(
                    get(),
                    AndroidPdfPages(contentResolver, get()) { get<AppPreferences>().localFolder.value },
                    // RF-13: CBR and CB7 unpacked to the app's cache (ADR-011).
                    archives = JvmArchiveUnpacker(get(), java.io.File(cacheDir, "archives")),
                )
            }
            single {
                val prefs = get<AppPreferences>()
                MangaDexSource(
                    get(),
                    contentLanguages = { prefs.mangaDexLanguages() },
                    contentRatings = { prefs.mangaDexRatings() },
                    dataSaver = { prefs.mangaDexDataSaver.value },
                )
            }
            // RF-12: HQ Now only on Android; the browser needs the bridge for its images (FEAT-051).
            single { HqNowSource(get(), apiUrl = { get<SourceConfig>().baseUrl(SourceId.HQ_NOW) ?: HqNowSource.API }) }
            single<SourceListFetcher> { HttpSourceListFetcher(get()) }
            single<SecretStore> { KeystoreSecretStore(this@HqrApplication) }
            // RF-18: book files from servers on disk, read by ranges (never whole in memory).
            single<BookStore> { DiskBookStore(FileSystem.SYSTEM, cacheDir.toOkioPath() / "books") }
            // RF-62 to RF-66: chapter files in the app's own storage, run by WorkManager (ADR-009).
            single { DownloadFiles(FileSystem.SYSTEM, filesDir.toOkioPath() / "downloads") }
            single<DownloadScheduler> { WorkManagerDownloadScheduler(this@HqrApplication, get()) }
            // RF-67 and RF-68: the copy in the chosen folder and the free space check.
            single<DownloadExport> { SafDownloadExport(this@HqrApplication) }
            single<FreeSpace> { AndroidFreeSpace(filesDir) }
            single<Connectivity> { AndroidConnectivity(this@HqrApplication) }
            // RF-82: the GitHub flavor updates itself (ADR-015).
            single<Updates> { AndroidUpdates(this@HqrApplication, ReleaseChecker(get(), get(), BuildConfig.VERSION_NAME), get()) }
            single(AppVersion) { BuildConfig.VERSION_NAME }
            CrashReporting.reports?.let { reports -> single<CrashReports> { reports } }
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
        }

    /** RF-10: HQR/<version> (<site>). */
    private fun userAgent(): String = "HQR/${BuildConfig.VERSION_NAME} (https://github.com/tiagohs/hqr-comics-reader)"
}
