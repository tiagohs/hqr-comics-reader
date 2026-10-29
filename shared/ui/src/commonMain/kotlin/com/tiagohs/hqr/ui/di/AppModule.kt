package com.tiagohs.hqr.ui.di

import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.MangaReaderSettingsEntity
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.bytes
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.CategoryRepository
import com.tiagohs.hqr.domain.repository.ChapterRepository
import com.tiagohs.hqr.domain.repository.HistoryRepository
import com.tiagohs.hqr.domain.repository.MangaRepository
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel
import com.tiagohs.hqr.presentation.history.HistoryViewModel
import com.tiagohs.hqr.presentation.library.LibraryViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.presentation.reader.ChapterPages
import com.tiagohs.hqr.presentation.reader.PageScale
import com.tiagohs.hqr.presentation.reader.ReaderBackground
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.presentation.reader.ReaderSettings
import com.tiagohs.hqr.presentation.reader.ReaderSettingsStore
import com.tiagohs.hqr.presentation.reader.ReaderViewModel
import com.tiagohs.hqr.presentation.reader.TapZones
import com.tiagohs.hqr.sources.local.LocalSource
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Qualifier of the list of built-in sources each platform provides (ADR-003). */
val BuiltInSources = named("builtInSources")

/** What every platform shares; the platform module brings database, settings, files, HTTP and the sources. */
val coreModule: Module =
    module {
        single<Clock> { Clock.System }
        single { AppPreferences(get()) }
        single<MangaRepository> { RoomMangaRepository(get(), get()) }
        single<ChapterRepository> { RoomChapterRepository(get(), get()) }
        single<CategoryRepository> { RoomCategoryRepository(get()) }
        single<HistoryRepository> { RoomHistoryRepository(get(), get(), get()) }
        single { SourceRegistry(get<List<Source>>(BuiltInSources)) }
        single<ImageBytes> { AppImageBytes(get(), get(), get()) }
        single<ReaderSettingsStore> { DbReaderSettingsStore(get(), get()) }
        single<ChapterPages> {
            val registry = get<SourceRegistry>()
            ChapterPages { manga, chapter -> registry.require(manga.sourceId).pages(manga.source, chapter.source) }
        }
        viewModel { LibraryViewModel(get(), get(), get()) }
        viewModel { HistoryViewModel(get(), get()) }
        viewModel { (id: SourceId) -> BrowseSourceViewModel(id, get(), get()) }
        viewModel { (id: Long) -> MangaViewModel(id, get(), get(), get()) }
        viewModel { (mangaId: Long, chapterId: Long, page: Int) ->
            ReaderViewModel(mangaId, chapterId, page, get(), get(), get(), get(), get(), get())
        }
    }

/** Images: the cache first, then the source's own loader or a plain GET (ADR-006). */
class AppImageBytes(
    private val registry: SourceRegistry,
    private val http: HttpClient,
    private val cache: ImageCache,
) : ImageBytes {
    override suspend fun load(
        sourceId: SourceId,
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        val clean = headers.filterKeys { !it.startsWith("x-attempt") }
        if (url.startsWith(LocalSource.SCHEME)) return (registry.require(SourceId.LOCAL) as ImageSource).image(url, clean)
        cache.get(url)?.let { return it }
        val source = registry[sourceId]
        val bytes = if (source is ImageSource) source.image(url, clean) else http.bytes(url, clean)
        cache.put(url, bytes)
        return bytes
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
        val base = global() ?: ReaderSettings(mode = ReaderSettings.suggestedMode(manga.source.kind))
        val own = db.readerSettingsDao().get(manga.id) ?: return if (global() == null) base else base.copy(mode = base.mode)
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
    }

    override suspend fun saveGlobal(settings: ReaderSettings) {
        prefs.putString(KEY, json.encodeToString(ReaderSettings.serializer(), settings))
    }

    private companion object {
        const val KEY = "reader_settings"
    }
}
