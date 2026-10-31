package com.tiagohs.hqr.app

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.download.DownloadFiles
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.domain.repository.DownloadSettings
import com.tiagohs.hqr.domain.repository.Downloads
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.color.CoverPixels
import com.tiagohs.hqr.presentation.downloads.DownloadsViewModel
import com.tiagohs.hqr.presentation.feed.FeedViewModel
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.presentation.library.CategoriesViewModel
import com.tiagohs.hqr.presentation.library.LibraryOptions
import com.tiagohs.hqr.presentation.library.LibraryOptionsStore
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.settings.BackupViewModel
import com.tiagohs.hqr.presentation.sources.AddServerViewModel
import com.tiagohs.hqr.presentation.sources.ServerCatalogViewModel
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import com.tiagohs.hqr.sources.local.LocalSource
import com.tiagohs.hqr.sources.local.OkioFileTree
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The graph every platform starts (architecture §3.1): the iOS app asks for each ViewModel by name through
 * SharedGraph, so a definition that no longer resolves must fail here, on the JVM, before it reaches the iPhone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedGraphTest {
    private lateinit var koin: Koin
    private val fs = FakeFileSystem().apply { createDirectories("/comics".toPath()) }

    @BeforeTest
    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val platform =
            module {
                single<HqrDatabase> { inMemoryDatabase() }
                single<Settings> { PropertiesSettings(java.util.Properties()) }
                single<HttpClient> { hqrHttpClient("HQR/test") }
                single<ImageCache> { MemoryImageCache() }
                single<List<Source>>(BuiltInSources) { listOf(LocalSource(OkioFileTree(fs, "/comics".toPath()))) }
                single<SourceListFetcher> { SourceListFetcher { null } }
                single<SecretStore> { SettingsSecretStore(get()) }
                single<CoverPixels> { CoverPixels { null } }
                single(AppVersion) { "1.0.0" }
                single { DownloadFiles(fs, "/downloads".toPath()) }
                single<DownloadScheduler> { DownloadScheduler { } }
            }
        koin = startKoin { modules(platform, coreModule, downloadModule) }.koin
    }

    @AfterTest
    fun stop() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun FEAT060_every_screen_view_model_resolves() {
        val local = SourceId.LOCAL
        listOf(
            koin.screenViewModel<LibraryViewModel>(),
            koin.screenViewModel<CategoriesViewModel>(),
            koin.screenViewModel<HistoryViewModel>(),
            koin.screenViewModel<FeedViewModel>(),
            koin.screenViewModel<GlobalSearchViewModel>(),
            koin.screenViewModel<BrowseSourceViewModel>(local, null),
            koin.screenViewModel<MangaViewModel>(1L),
            koin.screenViewModel<MigrationViewModel>(1L),
            koin.screenViewModel<ReaderViewModel>(1L, 1L, 0),
            koin.screenViewModel<DownloadsViewModel>(),
            koin.screenViewModel<SourcesViewModel>(),
            koin.screenViewModel<AddServerViewModel>(null),
            koin.screenViewModel<ServerCatalogViewModel>(SourceId("opds-1")),
            koin.screenViewModel<BackupViewModel>(),
        ).forEach { assertNotNull(it) }
        assertTrue(koin.get<Downloads>() is DownloadManager)
    }

    @Test
    fun FEAT060_startup_marks_the_sources_ready() =
        runTest {
            koin.get<AppStartup>().run()
            assertEquals(SourceId.LOCAL, koin.get<SourceRegistry>().awaitRequire(SourceId.LOCAL).id)
        }

    @Test
    fun RF54_library_options_survive_a_restart() {
        val store = koin.get<LibraryOptionsStore>()
        store.save(LibraryOptions(unreadBadge = false, sourceBadge = true))
        assertEquals(LibraryOptions(unreadBadge = false, sourceBadge = true), store.load())
    }

    @Test
    fun RF64_download_settings_write_through_to_the_preferences() {
        val settings = koin.get<DownloadSettings>()
        settings.setWifiOnly(true)
        settings.setDeleteAfterRead(true)
        settings.setSaveAsCbz(true)
        settings.setFolder("content://folder")
        assertTrue(settings.wifiOnly.value && settings.deleteAfterRead.value && settings.saveAsCbz.value)
        assertEquals("content://folder", settings.folder.value)
    }
}
