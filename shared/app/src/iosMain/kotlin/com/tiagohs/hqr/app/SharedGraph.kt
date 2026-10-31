@file:OptIn(ExperimentalForeignApi::class, ExperimentalSettingsImplementation::class)

package com.tiagohs.hqr.app

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.iosDatabase
import com.tiagohs.hqr.data.download.DownloadFiles
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.net.ChallengeCookies
import com.tiagohs.hqr.data.net.DiskImageCache
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.net.useChallengeCookies
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.data.sources.HttpSourceListFetcher
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.downloads.DownloadsViewModel
import com.tiagohs.hqr.presentation.feed.FavoriteSources
import com.tiagohs.hqr.presentation.feed.FeedViewModel
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.settings.BackupViewModel
import com.tiagohs.hqr.presentation.sources.AddServerViewModel
import com.tiagohs.hqr.presentation.sources.ServerCatalogViewModel
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import com.tiagohs.hqr.sources.archive.InternetArchiveSource
import com.tiagohs.hqr.sources.hqnow.HqNowSource
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.local.OkioFileTree
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.sources.mangaflix.MangaFlixSource
import com.tiagohs.hqr.sources.namicomi.NamiComiSource
import com.tiagohs.hqr.sources.opds.BookStore
import com.tiagohs.hqr.sources.opds.DiskBookStore
import com.tiagohs.hqr.sources.weebcentral.WeebCentralSource
import com.tiagohs.hqr.sources.xoxo.XoxoSource
import com.tiagohs.hqr.sources.yomu.YomuSource
import io.ktor.client.HttpClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.posix.memcpy

/**
 * What the iPhone and iPad app calls (Swift: `SharedGraph.shared`): [start] creates the graph once per process, then
 * each screen asks for its ViewModel here (Swift cannot use Koin's reified calls). Source ids cross as plain strings.
 */
@Suppress("TooManyFunctions") // One factory per screen is the point of this object.
object SharedGraph {
    private var started: Koin? = null

    private val koin: Koin get() = started ?: start("1.0.0", false)

    /**
     * Called once by the app at launch with its version (RF-105) and whether it runs in the UI tests (an in-memory
     * library, no remote source list); later calls return the graph already running.
     */
    fun start(
        version: String,
        uiTest: Boolean,
    ): Koin {
        started?.let { return it }
        val graph = startKoin { modules(iosModule(version, uiTest), coreModule, downloadModule) }.koin
        started = graph
        MainScope().launch { graph.get<AppStartup>().run() }
        return graph
    }

    fun libraryViewModel(): LibraryViewModel = koin.screenViewModel()

    fun categoriesViewModel(): CategoriesViewModel = koin.screenViewModel()

    fun historyViewModel(): HistoryViewModel = koin.screenViewModel()

    fun feedViewModel(): FeedViewModel = koin.screenViewModel()

    fun globalSearchViewModel(): GlobalSearchViewModel = koin.screenViewModel()

    /** [query] opens the source already searching (RF-23); empty for its catalog. */
    fun browseSourceViewModel(
        sourceId: String,
        query: String,
    ): BrowseSourceViewModel = koin.screenViewModel(SourceId(sourceId), query.ifEmpty { null })

    fun mangaViewModel(mangaId: Long): MangaViewModel = koin.screenViewModel(mangaId)

    fun migrationViewModel(mangaId: Long): MigrationViewModel = koin.screenViewModel(mangaId)

    fun readerViewModel(
        mangaId: Long,
        chapterId: Long,
        page: Int,
    ): ReaderViewModel = koin.screenViewModel(mangaId, chapterId, page)

    fun downloadsViewModel(): DownloadsViewModel = koin.screenViewModel()

    fun sourcesViewModel(): SourcesViewModel = koin.screenViewModel()

    /** [serverId] edits a saved server; empty adds a new one (RF-17). */
    fun addServerViewModel(serverId: String): AddServerViewModel = koin.screenViewModel(serverId.ifEmpty { null }?.let(::SourceId))

    fun serverCatalogViewModel(serverId: String): ServerCatalogViewModel = koin.screenViewModel(SourceId(serverId))

    fun backupViewModel(): BackupViewModel = koin.screenViewModel()

    /** RF-115: the favorite sources' ids, as they change. */
    fun favoriteSourceIds(): Observer<List<String>> =
        Observer(
            koin
                .get<FavoriteSources>()
                .observe()
                .map { ids -> ids.map { it.value }.sorted() }
                .stateIn(MainScope(), SharingStarted.Eagerly, emptyList()),
        )

    /** RF-115: "added", "removed" or "full" (the feed already has 5 items). */
    suspend fun toggleFavoriteSource(sourceId: String): String =
        when (koin.get<FavoriteSources>().toggle(SourceId(sourceId))) {
            FavoriteSources.Result.ADDED -> "added"
            FavoriteSources.Result.REMOVED -> "removed"
            FavoriteSources.Result.FEED_FULL -> "full"
        }

    /** RF-66: free bytes where the downloads go; -1 when the system does not say. */
    fun freeBytes(): Long = koin.getOrNull<com.tiagohs.hqr.data.download.FreeSpace>()?.bytes() ?: -1L

    fun preferences(): AppPreferences = koin.get()

    fun sources(): SourceRegistry = koin.get()

    fun sourceConfig(): SourceConfig = koin.get()

    /** Pages and covers: downloaded files, the cache, then the network (ADR-006). */
    fun images(): ImageBytes = koin.get()

    fun imageCache(): ImageCache = koin.get()

    fun challengeCookies(): IosChallengeCookies = koin.get()

    fun connectivity(): Connectivity = koin.get()

    /** The download queue, for the background task the app registers (RNF-28). */
    fun downloadScheduler(): IosDownloadScheduler = koin.get()

    /** The folder Files shows as "HQR" (RF-16): the local source reads comics put there. */
    fun localFolder(): String = documents()

    /** Bytes from Kotlin as Data for UIImage and ImageIO. */
    fun data(bytes: ByteArray): NSData =
        if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
        }

    /** Data from Swift (a backup file picked in Files) as Kotlin bytes. */
    fun bytes(data: NSData): ByteArray =
        ByteArray(data.length.toInt()).also { out ->
            if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
}

private fun userAgent(version: String) = "HQR/$version (https://github.com/tiagohs/hqr-comics-reader)"

@Suppress("LongMethod") // One registration per line: the iOS app's whole wiring in one place.
private fun iosModule(
    version: String,
    uiTest: Boolean,
): Module =
    module {
        single<HqrDatabase> {
            if (uiTest) {
                com.tiagohs.hqr.data.db
                    .inMemoryDatabase()
            } else {
                iosDatabase()
            }
        }
        single<Settings> {
            if (uiTest) {
                NSUserDefaultsSettings(NSUserDefaults(suiteName = "hqr.uitests").apply { removePersistentDomainForName("hqr.uitests") })
            } else {
                NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
            }
        }
        single { IosChallengeCookies(get()) }
        single<ChallengeCookies> { get<IosChallengeCookies>() }
        single<HttpClient> { hqrHttpClient(userAgent(version)) { useChallengeCookies(get()) } }
        single<ImageCache> { DiskImageCache(FileSystem.SYSTEM, caches().toPath() / "images") }
        single<FileTree> { OkioFileTree(FileSystem.SYSTEM, documents().toPath()) }
        // RF-13, RF-14: CBZ, folders of images and PDF; CBR and CB7 stay Android-only (ADR-011).
        single { LocalSource(get(), IosPdfPages(documents())) }
        single {
            val prefs = get<AppPreferences>()
            MangaDexSource(
                get(),
                contentLanguages = { prefs.mangaDexLanguages() },
                contentRatings = { prefs.mangaDexRatings() },
                dataSaver = { prefs.mangaDexDataSaver.value },
            )
        }
        single { HqNowSource(get(), apiUrl = { get<SourceConfig>().baseUrl(SourceId.HQ_NOW) ?: HqNowSource.API }) }
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
        single<SourceListFetcher> { if (uiTest) SourceListFetcher { null } else HttpSourceListFetcher(get()) }
        // RF-17: server passwords in the Keychain, never in the backup (RF-72).
        single<SecretStore> { SettingsSecretStore(KeychainSettings("com.tiagohs.hqr.secrets")) }
        single<BookStore> { DiskBookStore(FileSystem.SYSTEM, caches().toPath() / "books") }
        // RF-62 to RF-66: chapter files in Application Support (out of Files, kept by the device backup).
        single { DownloadFiles(FileSystem.SYSTEM, support().toPath() / "downloads") }
        single { IosConnectivity() }
        single<Connectivity> { get<IosConnectivity>() }
        single {
            val prefs = get<AppPreferences>()
            IosDownloadScheduler({ get<DownloadManager>() }, get(), { prefs.wifiOnlyDownloads.value })
        }
        single<DownloadScheduler> { get<IosDownloadScheduler>() }
        single<FreeSpace> { IosFreeSpace(support()) }
        single<CoverPixels> { IosCoverPixels(get()) }
        single(AppVersion) { version }
    }

private fun directory(kind: ULong): String {
    val path = NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true).first() as String
    NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    return path
}

private fun documents() = directory(NSDocumentDirectory)

private fun caches() = directory(NSCachesDirectory)

private fun support() = directory(platform.Foundation.NSApplicationSupportDirectory)
