package com.tiagohs.hqr.ui.di

import com.tiagohs.hqr.data.backup.BackupService
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.MangaReaderSettingsEntity
import com.tiagohs.hqr.data.download.DownloadFiles
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.bytes
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.data.repository.RoomSearchRepository
import com.tiagohs.hqr.data.repository.RoomTransactor
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.data.sources.SourceConfig
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.Backups
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.DownloadSettings
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.downloads.DownloadsViewModel
import com.tiagohs.hqr.presentation.feed.FeedViewModel
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryOptionsStore
import com.tiagohs.hqr.presentation.library.LibraryUpdater
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.presentation.migration.WorkMigration
import com.tiagohs.hqr.presentation.reader.ChapterPages
import com.tiagohs.hqr.presentation.reader.PagePrefetcher
import com.tiagohs.hqr.presentation.reader.PageScale
import com.tiagohs.hqr.presentation.reader.ReaderBackground
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderSettings
import com.tiagohs.hqr.presentation.reader.ReaderSettingsStore
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.reader.TapZones
import com.tiagohs.hqr.presentation.settings.BackupViewModel
import com.tiagohs.hqr.presentation.settings.UpdateViewModel
import com.tiagohs.hqr.presentation.sources.AddServerViewModel
import com.tiagohs.hqr.presentation.sources.BridgeViewModel
import com.tiagohs.hqr.presentation.sources.ServerCatalogViewModel
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.opds.BookStore
import com.tiagohs.hqr.sources.opds.MemoryBookStore
import com.tiagohs.hqr.sources.opds.OpdsServers
import com.tiagohs.hqr.ui.color.ImageCoverPixels
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Qualifier of the list of built-in sources each platform provides (ADR-003). */
val BuiltInSources = named("builtInSources")

/** Qualifier of the app version string ("1.0.0"), for the minimum version in `sources.json` (RF-105). */
val AppVersion = named("appVersion")

/** What every platform shares; the platform module brings database, settings, files, HTTP and the sources. */
val coreModule: Module =
    module {
        single<Clock> { Clock.System }
        single { AppPreferences(get()) }
        single<MangaRepository> { RoomMangaRepository(get(), get()) }
        single<ChapterRepository> { RoomChapterRepository(get(), get()) }
        single<CategoryRepository> { RoomCategoryRepository(get()) }
        single<HistoryRepository> { RoomHistoryRepository(get(), get(), get()) }
        single<SearchRepository> { RoomSearchRepository(get(), get()) }
        // Not ready until HqrApp applied the saved servers and switches (RF-88).
        single { SourceRegistry(get<List<Source>>(BuiltInSources), ready = false) }
        single { SourceConfig(get(), get(), get(), get(), get(AppVersion)) }
        single<SourceSwitches> { get<SourceConfig>() }
        viewModel { SourcesViewModel(get(), get(), get()) }
        single<Backups> {
            val registry = get<SourceRegistry>()
            BackupService(
                get(),
                get(),
                "HQR ${get<String>(AppVersion)}",
                { registry.everything.associate { it.id.value to it.name } },
                sources = { registry.builtIn },
            )
        }
        viewModel { BackupViewModel(get()) }
        viewModel { UpdateViewModel(get()) }
        viewModel { BridgeViewModel(get()) }
        single<ServerManager> { OpdsServers(get(), get(), get(), get(), getOrNull<BookStore>() ?: MemoryBookStore()) }
        viewModel { (id: SourceId?) -> AddServerViewModel(get(), id, get()) }
        viewModel { (id: SourceId) -> ServerCatalogViewModel(id, get(), get(), get()) }
        single<ImageBytes> { AppImageBytes(get(), get(), get(), getOrNull()) }
        single<ReaderSettingsStore> { DbReaderSettingsStore(get(), get()) }
        single<ChapterPages> {
            val registry = get<SourceRegistry>()
            val downloads = getOrNull<DownloadManager>()
            // RF-65: a downloaded chapter is read from its files, also without internet.
            ChapterPages { manga, chapter ->
                downloads?.offlinePages(chapter.id) ?: registry.awaitRequire(manga.sourceId).pages(manga.source, chapter.source)
            }
        }
        single<LibraryOptionsStore> { PrefsLibraryOptionsStore(get()) }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                chapters = get(),
                downloads = getOrNull(),
                updater = LibraryUpdater(get(), get(), get()),
            )
        }
        viewModel { CategoriesViewModel(get(), get()) }
        viewModel { HistoryViewModel(get(), get()) }
        viewModel { (id: SourceId, query: String?) -> BrowseSourceViewModel(id, get(), get(), query, get(), get()) }
        viewModel { FeedViewModel(get(), get(), get()) }
        viewModel { GlobalSearchViewModel(get(), get(), get(), searches = get()) }
        single<CoverPixels> { ImageCoverPixels(get()) }
        single<PagePrefetcher> {
            val images = get<ImageBytes>()
            PagePrefetcher { manga, page -> images.load(manga.sourceId, page.imageUrl, page.headers) }
        }
        viewModel { (id: Long) ->
            MangaViewModel(
                id,
                get(),
                get(),
                get(),
                get(),
                get(),
                getOrNull(),
                getOrNull<Connectivity>() ?: Connectivity.Always,
            )
        }
        // RF-69: the downloads only where the app has them (Android).
        factory { WorkMigration(get(), get(), get(), get(), getOrNull(), getOrNull(), RoomTransactor(get())) }
        viewModel { (id: Long) -> MigrationViewModel(id, get(), get(), get()) }
        single<DownloadSettings> {
            val prefs = get<AppPreferences>()
            object : DownloadSettings {
                override val wifiOnly = prefs.wifiOnlyDownloads
                override val deleteAfterRead = prefs.deleteAfterRead
                override val saveAsCbz = prefs.downloadsAsCbz
                override val folder = prefs.downloadFolder

                override fun setWifiOnly(on: Boolean) = prefs.setWifiOnlyDownloads(on)

                override fun setSaveAsCbz(on: Boolean) = prefs.setDownloadsAsCbz(on)

                override fun setFolder(uri: String?) = prefs.setDownloadFolder(uri)

                override fun setDeleteAfterRead(on: Boolean) = prefs.setDeleteAfterRead(on)
            }
        }
        viewModel { (mangaId: Long, chapterId: Long, page: Int) ->
            ReaderViewModel(mangaId, chapterId, page, get(), get(), get(), get(), get(), get(), get(), get(), getOrNull())
        }
    }

/**
 * Downloads where the platform has files (Android): the queue of ADR-009 run by the platform's [DownloadScheduler].
 * Pages are fetched straight from the source, never through the reading cache.
 */
val downloadModule: Module =
    module {
        single {
            val registry = get<SourceRegistry>()
            val mangas = get<MangaRepository>()
            val chapters = get<ChapterRepository>()
            val http = get<HttpClient>()
            val settings = get<DownloadSettings>()

            suspend fun target(chapterId: Long): Pair<Manga, Chapter> {
                val chapter = chapters.get(chapterId) ?: error("Capítulo apagado")
                val manga = mangas.get(chapter.mangaId) ?: error("Obra apagada")
                return manga to chapter
            }
            DownloadManager(
                get(),
                get(),
                get(),
                pagesOf = { id ->
                    val (manga, chapter) = target(id)
                    registry.awaitRequire(manga.sourceId).pages(manga.source, chapter.source)
                },
                bytesOf = { id, page ->
                    val source = registry.awaitRequire(target(id).first.sourceId)
                    if (source is ImageSource) source.image(page.imageUrl, page.headers) else http.bytes(page.imageUrl, page.headers)
                },
                deleteAfterRead = { settings.deleteAfterRead.value },
                saveAsCbz = { settings.saveAsCbz.value },
                folder = { settings.folder.value },
                export = getOrNull(),
                freeBytes = getOrNull<FreeSpace>()?.let { space -> { space.bytes() } } ?: { null },
            )
        }
        single<Downloads> { get<DownloadManager>() }
        viewModel { DownloadsViewModel(get(), get()) }
    }

/** Images: downloaded files, the cache, then the source's own loader or a plain GET (ADR-006). */
class AppImageBytes(
    private val registry: SourceRegistry,
    private val http: HttpClient,
    private val cache: ImageCache,
    private val files: DownloadFiles? = null,
) : ImageBytes {
    private val lock = Mutex()

    // The pager and the prefetch ask for the same page at once: one download, every caller waits for it.
    private val inFlight = HashMap<String, CompletableDeferred<ByteArray>>()

    // The waits and the hand-over of failures to the other callers are the point of this function.
    @Suppress("ReturnCount", "TooGenericExceptionCaught", "SwallowedException")
    override suspend fun load(
        sourceId: SourceId,
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        val clean = headers.filterKeys { !it.startsWith("x-attempt") }
        // RF-65: pages of a downloaded chapter, from the device.
        files?.bytes(url)?.let { return it }
        if (url.startsWith(LocalSource.SCHEME)) return (registry.require(SourceId.LOCAL) as ImageSource).image(url, clean)
        cache.get(url)?.let { return it }
        while (true) {
            val (pending, owner) =
                lock.withLock {
                    inFlight[url]?.let { it to false } ?: CompletableDeferred<ByteArray>().also { inFlight[url] = it } to true
                }
            if (!owner) {
                // The download we waited for was cancelled by its own caller: try again ourselves.
                try {
                    return pending.await()
                } catch (e: OwnerCancelled) {
                    continue
                }
            }
            try {
                registry.awaitReady()
                val source = registry[sourceId]
                val bytes = if (source is ImageSource) source.image(url, clean) else http.bytes(url, clean)
                cache.put(url, bytes)
                pending.complete(bytes)
                return bytes
            } catch (e: CancellationException) {
                pending.completeExceptionally(OwnerCancelled())
                throw e
            } catch (e: Throwable) {
                pending.completeExceptionally(e)
                throw e
            } finally {
                withContext(NonCancellable) { lock.withLock { inFlight.remove(url) } }
            }
        }
    }

    private class OwnerCancelled : RuntimeException()
}

/** Library filters, order and display, kept with the preferences (RF-54, RF-55). */
class PrefsLibraryOptionsStore(
    private val prefs: AppPreferences,
) : LibraryOptionsStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): LibraryOptions =
        prefs.getString(KEY)?.let { runCatching { json.decodeFromString(LibraryOptions.serializer(), it) }.getOrNull() } ?: LibraryOptions()

    override fun save(options: LibraryOptions) = prefs.putString(KEY, json.encodeToString(LibraryOptions.serializer(), options))

    private companion object {
        const val KEY = "library_options"
    }
}

/** Reader settings: global in the preferences, per work in the database (RF-37). */
class DbReaderSettingsStore(
    private val db: HqrDatabase,
    private val prefs: AppPreferences,
) : ReaderSettingsStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun global(): ReaderSettings? =
        prefs.getString(KEY)?.let {
            runCatching { json.decodeFromString(ReaderSettings.serializer(), it) }.getOrNull()
        }

    override suspend fun forManga(manga: Manga): ReaderSettings {
        // RF-37: the first read follows the kind of work (a Japanese manga right to left); the global mode is for works
        // of an unknown kind. A work's own choice always wins.
        val saved = global() ?: ReaderSettings()
        val suggested =
            manga.source.kind
                .takeIf { it != ReadingKind.UNKNOWN }
                ?.let(ReaderSettings::suggestedMode)
        val base = saved.copy(mode = suggested ?: saved.mode)
        val own = db.readerSettingsDao().get(manga.id) ?: return base
        return base.copy(
            mode = own.mode?.let { runCatching { ReaderMode.valueOf(it) }.getOrNull() } ?: ReaderSettings.suggestedMode(manga.source.kind),
            scale = own.scale?.let { runCatching { PageScale.valueOf(it) }.getOrNull() } ?: base.scale,
            tapZones = own.tapZones?.let { runCatching { TapZones.valueOf(it) }.getOrNull() } ?: base.tapZones,
            background = own.background?.let { runCatching { ReaderBackground.valueOf(it) }.getOrNull() } ?: base.background,
            doublePage = own.doublePage ?: base.doublePage,
            cropBorders = own.cropBorders ?: base.cropBorders,
        )
    }

    override suspend fun saveForManga(
        mangaId: Long,
        settings: ReaderSettings,
    ) {
        db.readerSettingsDao().upsert(
            MangaReaderSettingsEntity(
                mangaId = mangaId,
                mode = settings.mode.name,
                scale = settings.scale.name,
                tapZones = settings.tapZones.name,
                background = settings.background.name,
                doublePage = settings.doublePage,
                cropBorders = settings.cropBorders,
            ),
        )
        // The reader's own options (not the work's) go to everyone, so "Só esta obra" never loses them.
        val current = global() ?: ReaderSettings()
        saveGlobal(
            current.copy(
                keepScreenOn = settings.keepScreenOn,
                showPageNumber = settings.showPageNumber,
                volumeKeys = settings.volumeKeys,
                splitWide = settings.splitWide,
                invertPages = settings.invertPages,
                invertZones = settings.invertZones,
                preload = settings.preload,
            ),
        )
    }

    override suspend fun saveGlobal(settings: ReaderSettings) {
        prefs.putString(KEY, json.encodeToString(ReaderSettings.serializer(), settings))
    }

    private companion object {
        const val KEY = "reader_settings"
    }
}
