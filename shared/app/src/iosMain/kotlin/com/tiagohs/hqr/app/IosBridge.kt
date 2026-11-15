package com.tiagohs.hqr.app

import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.map

import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.manga.MangaViewModel
import com.tiagohs.hqr.presentation.migration.MigrationViewModel
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import platform.Foundation.NSData

/**
 * What Swift cannot reach on its own: source ids are a Kotlin value class (Swift gets plain strings here), collections
 * typed as `Collection` (Swift passes arrays), and image bytes (Swift gets Data).
 */
@Suppress("TooManyFunctions") // One small adapter per gap of the Objective-C export.
object IosBridge {
    fun sourceOf(manga: SourceManga): String = manga.sourceId.value

    fun sourceOfRow(row: SourcesViewModel.Row): String = row.id.value

    fun sourceOfGroup(group: GlobalSearchViewModel.Group): String = group.sourceId.value

    fun sourceOfOption(option: MigrationViewModel.SourceOption): String = option.id.value

    fun sourceOfServer(server: Server): String = server.id.value

    fun isLocal(manga: SourceManga): Boolean = manga.sourceId == SourceId.LOCAL

    /** The source's name; the local one is named by the screens in the app language. */
    fun sourceName(sourceId: String): String = SharedGraph.sources()[SourceId(sourceId)]?.name ?: sourceId

    fun sourceStatus(sourceId: String): SourceRegistry.Status = SharedGraph.sources().status(SourceId(sourceId))

    /** "10", "10.5": the number of a chapter as people write it. */
    fun chapterNumber(chapter: Chapter): String = chapter.source.number.chapterLabel()

    fun migrationResults(
        state: MigrationViewModel.State,
        sourceId: String,
    ): MigrationViewModel.Results? = state.results[SourceId(sourceId)]

    fun toggleMigrationSource(
        viewModel: MigrationViewModel,
        sourceId: String,
    ) = viewModel.toggleSource(SourceId(sourceId))

    fun setSourceEnabled(
        viewModel: SourcesViewModel,
        sourceId: String,
        enabled: Boolean,
    ) = viewModel.setEnabled(SourceId(sourceId), enabled)

    fun deleteServer(
        viewModel: SourcesViewModel,
        sourceId: String,
    ) = viewModel.deleteServer(SourceId(sourceId))

    fun retrySearch(
        viewModel: GlobalSearchViewModel,
        sourceId: String,
    ) = viewModel.retry(SourceId(sourceId))

    fun download(
        viewModel: MangaViewModel,
        chapterIds: List<Long>,
    ) = viewModel.download(chapterIds)

    fun removeDownloads(
        viewModel: MangaViewModel,
        chapterIds: List<Long>,
    ) = viewModel.removeDownloads(chapterIds)

    fun setRead(
        viewModel: MangaViewModel,
        chapterIds: List<Long>,
        read: Boolean,
    ) = viewModel.setRead(chapterIds, read)

    fun setCategories(
        viewModel: MangaViewModel,
        categoryIds: List<Long>,
    ) = viewModel.setCategories(categoryIds.toSet())

    /** RF-57: the picked works go to exactly these categories. */
    fun moveSelected(
        viewModel: com.tiagohs.hqr.presentation.library.LibraryViewModel,
        categoryIds: List<Long>,
    ) = viewModel.moveSelected(categoryIds.toSet())

    /** RF-58: the running update as zero or one item (Swift's observer takes no null). */
    fun libraryUpdate(viewModel: com.tiagohs.hqr.presentation.library.LibraryViewModel) = oneOrNone(viewModel.update)

    /** RF-58: the last finished update, for "N capítulos novos", as zero or one item. */
    fun libraryUpdated(viewModel: com.tiagohs.hqr.presentation.library.LibraryViewModel) = oneOrNone(viewModel.updated)

    private fun <T : Any> oneOrNone(flow: kotlinx.coroutines.flow.StateFlow<T?>): Observer<List<T>> =
        Observer(
            flow
                .map { listOfNotNull(it) }
                .stateIn(kotlinx.coroutines.MainScope(), kotlinx.coroutines.flow.SharingStarted.Eagerly, listOfNotNull(flow.value)),
        )

    /** SCR-EXP-004: every saved search, to put in the feed. */
    fun feedSavedSearches(viewModel: com.tiagohs.hqr.presentation.feed.FeedViewModel) =
        Observer(viewModel.allSaved.stateIn(kotlinx.coroutines.MainScope(), kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList()))

    /** SCR-EXP-004: the active sources that have a "latest" list. */
    fun catalogSourceIds(): List<String> =
        SharedGraph
            .sources()
            .active.value
            .filterIsInstance<com.tiagohs.hqr.domain.source.CatalogSource>()
            .map { it.id.value }

    fun feedItemSource(item: com.tiagohs.hqr.domain.repository.FeedItem): String = item.sourceId.value

    fun savedSearchSource(search: com.tiagohs.hqr.domain.repository.SavedSearch): String = search.sourceId.value

    /** RF-25: a source's latest ([savedSearchId] null) or a saved search into the feed. */
    fun addToFeed(
        viewModel: com.tiagohs.hqr.presentation.feed.FeedViewModel,
        sourceId: String,
        savedSearchId: Long?,
    ) = viewModel.add(SourceId(sourceId), savedSearchId)

    /** RF-53: every category, for the picker. */
    fun categoriesOf(choice: Pair<List<com.tiagohs.hqr.domain.model.Category>, Set<Long>>): List<com.tiagohs.hqr.domain.model.Category> =
        choice.first

    /** RF-53: the categories the work is in. */
    fun assignedOf(choice: Pair<List<com.tiagohs.hqr.domain.model.Category>, Set<Long>>): List<Long> = choice.second.toList()

    /** A cover or any image of a source, from the cache or the network (ADR-006). */
    suspend fun image(
        sourceId: String,
        url: String,
        headers: Map<String, String>,
    ): NSData = SharedGraph.data(SharedGraph.images().load(SourceId(sourceId), url, headers))

    suspend fun cover(manga: SourceManga): NSData? = manga.coverUrl?.let { image(manga.sourceId.value, it, emptyMap()) }

    /** A page of the reader: downloaded file, cache or network, with the source's headers (RF-65). */
    suspend fun page(
        manga: Manga,
        page: SourcePage,
    ): NSData = image(manga.sourceId.value, page.imageUrl, page.headers)

    // Sealed interfaces nest awkwardly in the Objective-C export: Swift reads them through these.

    /** The results of one source in the global search, or null while it loads or after a failure. */
    fun groupResults(group: GlobalSearchViewModel.Group): List<SourceManga>? =
        (group.state as? GlobalSearchViewModel.GroupState.Results)?.items

    fun groupError(group: GlobalSearchViewModel.Group): com.tiagohs.hqr.presentation.common.ErrorKind? =
        (group.state as? GlobalSearchViewModel.GroupState.Error)?.kind

    /** RF-30: the chapter the main button opens, or null when there is nothing to read. */
    fun actionChapter(action: com.tiagohs.hqr.domain.model.ReadAction): Chapter? =
        when (action) {
            is com.tiagohs.hqr.domain.model.ReadAction.Start -> action.chapter
            is com.tiagohs.hqr.domain.model.ReadAction.Continue -> action.chapter
            is com.tiagohs.hqr.domain.model.ReadAction.ReadAgain -> action.chapter
            else -> null
        }

    /** The page the main button opens at (0 unless continuing). */
    fun actionPage(action: com.tiagohs.hqr.domain.model.ReadAction): Int =
        (action as? com.tiagohs.hqr.domain.model.ReadAction.Continue)?.page ?: 0

    /** "start", "continue", "again" or "nothing": which label the main button shows. */
    fun actionKind(action: com.tiagohs.hqr.domain.model.ReadAction): String =
        when (action) {
            is com.tiagohs.hqr.domain.model.ReadAction.Start -> "start"
            is com.tiagohs.hqr.domain.model.ReadAction.Continue -> "continue"
            is com.tiagohs.hqr.domain.model.ReadAction.ReadAgain -> "again"
            else -> "nothing"
        }

    /** The server test: true when it answered, the problem otherwise, null before testing. */
    fun serverOk(state: com.tiagohs.hqr.presentation.sources.AddServerViewModel.State): Boolean =
        state.result is com.tiagohs.hqr.presentation.sources.AddServerViewModel.Result.Ok

    fun serverProblem(state: com.tiagohs.hqr.presentation.sources.AddServerViewModel.State): com.tiagohs.hqr.domain.source.ServerProblem? =
        (state.result as? com.tiagohs.hqr.presentation.sources.AddServerViewModel.Result.Problem)?.problem

    /** SCR-RDR-002: one reader option changed, the others kept (Swift passes null for what stays). */
    @Suppress("LongParameterList") // One parameter per option of the sheet.
    fun changeReaderSettings(
        settings: com.tiagohs.hqr.presentation.reader.ReaderSettings,
        mode: com.tiagohs.hqr.presentation.reader.ReaderMode?,
        scale: com.tiagohs.hqr.presentation.reader.PageScale?,
        background: com.tiagohs.hqr.presentation.reader.ReaderBackground?,
        tapZones: com.tiagohs.hqr.presentation.reader.TapZones?,
        doublePage: Boolean?,
        keepScreenOn: Boolean?,
        showPageNumber: Boolean?,
    ): com.tiagohs.hqr.presentation.reader.ReaderSettings =
        settings.copy(
            mode = mode ?: settings.mode,
            scale = scale ?: settings.scale,
            background = background ?: settings.background,
            tapZones = tapZones ?: settings.tapZones,
            doublePage = doublePage ?: settings.doublePage,
            keepScreenOn = keepScreenOn ?: settings.keepScreenOn,
            showPageNumber = showPageNumber ?: settings.showPageNumber,
        )

    /** RF-44, RF-45: cut margins, split wide pages in portrait, the other page first. */
    fun changePageOptions(
        settings: com.tiagohs.hqr.presentation.reader.ReaderSettings,
        cropBorders: Boolean?,
        splitWide: Boolean?,
        invertPages: Boolean?,
    ): com.tiagohs.hqr.presentation.reader.ReaderSettings =
        settings.copy(
            cropBorders = cropBorders ?: settings.cropBorders,
            splitWide = splitWide ?: settings.splitWide,
            invertPages = invertPages ?: settings.invertPages,
        )

    /** Fixed works and chapters for the SwiftUI previews (no network, no database). */
    fun sampleManga(
        title: String,
        sourceId: String,
    ): SourceManga =
        SourceManga(
            sourceId = SourceId(sourceId),
            key = title.lowercase().replace(' ', '-'),
            title = title,
            authors = listOf("Hirohiko Araki"),
            genres = listOf("Action", "Adventure"),
            description = "A sample work for the previews.",
        )

    fun sampleWork(
        id: Long,
        title: String,
    ): Manga = Manga(id, sampleManga(title, "mangadex"), inLibrary = true, addedAtEpochMs = 0)

    fun sampleChapter(
        id: Long,
        mangaId: Long,
        number: Double,
        read: Boolean,
    ): Chapter =
        Chapter(
            id,
            mangaId,
            com.tiagohs.hqr.domain.model
                .SourceChapter("c$id", number, "Sample"),
            read,
            0,
            SAMPLE_PAGES,
        )

    private const val SAMPLE_PAGES = 20
}
