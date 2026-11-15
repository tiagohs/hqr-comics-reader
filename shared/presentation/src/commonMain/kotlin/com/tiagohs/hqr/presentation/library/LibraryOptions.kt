package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.repository.LibraryItem
import kotlinx.serialization.Serializable

/** How the library looks (RF-54). */
@Serializable
enum class LibraryDisplay { COMPACT_GRID, COMFORTABLE_GRID, LIST }

/** Library order (RF-55). */
@Serializable
enum class LibrarySort { TITLE, LAST_READ, LATEST_CHAPTER, TOTAL_CHAPTERS, UNREAD, DATE_ADDED }

/** A filter that is off, required or excluded (tri-state, like the source filters). */
@Serializable
enum class TriState { OFF, ONLY, EXCLUDE }

/** Filter, order and display of the library, saved between sessions (SCR-LIB-002, RF-54, RF-55). */
@Serializable
data class LibraryOptions(
    val display: LibraryDisplay = LibraryDisplay.COMFORTABLE_GRID,
    val unreadBadge: Boolean = true,
    val downloadedBadge: Boolean = true,
    val sourceBadge: Boolean = false,
    val unread: TriState = TriState.OFF,
    val downloaded: TriState = TriState.OFF,
    val started: TriState = TriState.OFF,
    val ongoing: TriState = TriState.OFF,
    val completed: TriState = TriState.OFF,
    /** Category ids to show in "Todas" ([Category.DEFAULT_ID] is "Sem categoria"); empty shows all. */
    val categories: Set<Long> = emptySet(),
    /** Source ids to show; empty shows all. */
    val sources: Set<String> = emptySet(),
    val sort: LibrarySort = LibrarySort.TITLE,
    val ascending: Boolean = true,
) {
    /** "Limpar filtros" keeps the order and the display. */
    fun withoutFilters(): LibraryOptions =
        copy(
            unread = TriState.OFF,
            downloaded = TriState.OFF,
            started = TriState.OFF,
            ongoing = TriState.OFF,
            completed = TriState.OFF,
            sources = emptySet(),
            categories = emptySet(),
        )

    val hasFilters: Boolean get() = this != withoutFilters()

    /** The state filters of the "Filtrar" tab. */
    enum class StateFilter { UNREAD, DOWNLOADED, STARTED, ONGOING, COMPLETED }

    /** The marks on the covers of the "Exibir" tab. */
    enum class Badge { UNREAD, DOWNLOADED, SOURCE }

    fun isOn(filter: StateFilter): Boolean = state(filter) == TriState.ONLY

    fun toggle(filter: StateFilter): LibraryOptions {
        val next = if (isOn(filter)) TriState.OFF else TriState.ONLY
        return when (filter) {
            StateFilter.UNREAD -> copy(unread = next)
            StateFilter.DOWNLOADED -> copy(downloaded = next)
            StateFilter.STARTED -> copy(started = next)
            StateFilter.ONGOING -> copy(ongoing = next)
            StateFilter.COMPLETED -> copy(completed = next)
        }
    }

    fun toggleSource(id: String): LibraryOptions = copy(sources = if (id in sources) sources - id else sources + id)

    fun toggleCategory(id: Long): LibraryOptions = copy(categories = if (id in categories) categories - id else categories + id)

    /** RF-55: A to Z starts ascending; the other orders start with the most recent or the most. */
    fun withSort(value: LibrarySort): LibraryOptions =
        copy(sort = value, ascending = if (value == sort) ascending else value == LibrarySort.TITLE)

    fun withAscending(value: Boolean): LibraryOptions = copy(ascending = value)

    fun withDisplay(value: LibraryDisplay): LibraryOptions = copy(display = value)

    fun badgeOn(badge: Badge): Boolean =
        when (badge) {
            Badge.UNREAD -> unreadBadge
            Badge.DOWNLOADED -> downloadedBadge
            Badge.SOURCE -> sourceBadge
        }

    fun withBadge(
        badge: Badge,
        on: Boolean,
    ): LibraryOptions =
        when (badge) {
            Badge.UNREAD -> copy(unreadBadge = on)
            Badge.DOWNLOADED -> copy(downloadedBadge = on)
            Badge.SOURCE -> copy(sourceBadge = on)
        }

    private fun state(filter: StateFilter): TriState =
        when (filter) {
            StateFilter.UNREAD -> unread
            StateFilter.DOWNLOADED -> downloaded
            StateFilter.STARTED -> started
            StateFilter.ONGOING -> ongoing
            StateFilter.COMPLETED -> completed
        }
}

/** Where the options live; the platform keeps them with the other preferences. */
interface LibraryOptionsStore {
    fun load(): LibraryOptions

    fun save(options: LibraryOptions)
}

object LibraryRules {
    /** RF-55 and RF-56: filters, the text search (title, author, genre) and the order, in that sequence. */
    fun apply(
        items: List<LibraryItem>,
        options: LibraryOptions,
        query: String = "",
    ): List<LibraryItem> {
        val q = query.trim().lowercase()
        return items
            .filter { matches(options.unread, it.unread > 0) }
            .filter { matches(options.downloaded, it.downloaded > 0) }
            .filter { matches(options.started, it.lastReadAtEpochMs != null) }
            .filter { statusMatches(options, it.manga.source.status) }
            .filter { item ->
                options.categories.isEmpty() ||
                    item.categoryIds.any { it in options.categories } ||
                    (Category.DEFAULT_ID in options.categories && item.categoryIds.isEmpty())
            }.filter { options.sources.isEmpty() || it.manga.sourceId.value in options.sources }
            .filter { q.isEmpty() || it.searchText().contains(q) }
            .sortedWith(comparator(options))
    }

    /** "Em lançamento" and "Concluído" are one group: either one shows its works (or), excluded ones are hidden. */
    private fun statusMatches(
        options: LibraryOptions,
        status: PublicationStatus,
    ): Boolean {
        val wanted =
            buildSet {
                if (options.ongoing == TriState.ONLY) add(PublicationStatus.ONGOING)
                if (options.completed == TriState.ONLY) add(PublicationStatus.COMPLETED)
            }
        val excluded =
            buildSet {
                if (options.ongoing == TriState.EXCLUDE) add(PublicationStatus.ONGOING)
                if (options.completed == TriState.EXCLUDE) add(PublicationStatus.COMPLETED)
            }
        return (wanted.isEmpty() || status in wanted) && status !in excluded
    }

    private fun LibraryItem.searchText(): String =
        (listOf(manga.title) + manga.source.alternativeTitles + manga.source.authors + manga.source.artists + manga.source.genres)
            .joinToString("\n")
            .lowercase()

    private fun matches(
        state: TriState,
        value: Boolean,
    ) = when (state) {
        TriState.OFF -> true
        TriState.ONLY -> value
        TriState.EXCLUDE -> !value
    }

    private fun comparator(options: LibraryOptions): Comparator<LibraryItem> {
        val byTitle = compareBy<LibraryItem> { it.manga.title.lowercase() }
        val primary: Comparator<LibraryItem> =
            when (options.sort) {
                LibrarySort.TITLE -> byTitle
                LibrarySort.LAST_READ -> compareBy { it.lastReadAtEpochMs ?: Long.MIN_VALUE }
                LibrarySort.LATEST_CHAPTER -> compareBy { it.latestChapterAtEpochMs ?: Long.MIN_VALUE }
                LibrarySort.TOTAL_CHAPTERS -> compareBy { it.totalChapters }
                LibrarySort.UNREAD -> compareBy { it.unread }
                LibrarySort.DATE_ADDED -> compareBy { it.manga.addedAtEpochMs ?: Long.MIN_VALUE }
            }
        val ordered = if (options.ascending) primary else primary.reversed()
        // Ties keep A to Z, whatever the direction.
        return ordered.then(byTitle)
    }
}
