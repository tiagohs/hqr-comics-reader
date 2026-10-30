package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.LibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRulesTest {
    @Suppress("LongParameterList")
    private fun item(
        id: Long,
        title: String,
        unread: Int = 0,
        lastRead: Long? = null,
        latest: Long? = null,
        total: Int = 10,
        added: Long = id,
        downloaded: Int = 0,
        status: PublicationStatus = PublicationStatus.ONGOING,
        source: SourceId = SourceId.MANGADEX,
        authors: List<String> = emptyList(),
        genres: List<String> = emptyList(),
    ) = LibraryItem(
        Manga(id, SourceManga(source, "k$id", title, authors = authors, genres = genres, status = status), true, added),
        unread,
        downloaded,
        emptySet(),
        lastRead,
        latest,
        total,
    )

    private val items =
        listOf(
            item(1, "One Piece", unread = 3, lastRead = 30, authors = listOf("Eiichiro Oda"), genres = listOf("Aventura")),
            item(2, "Akira", lastRead = 10, status = PublicationStatus.COMPLETED, downloaded = 2),
            item(3, "Berserk", unread = 1, lastRead = 20, latest = 99),
            item(4, "Dandadan", unread = 5, source = SourceId.LOCAL),
        )

    private fun titles(
        options: LibraryOptions,
        query: String = "",
    ) = LibraryRules.apply(items, options, query).map { it.manga.title }

    @Test
    fun RF55_unreadSortedByLastRead() {
        // The SRS example: only works with unread, most recent reading first.
        assertEquals(
            listOf("One Piece", "Berserk", "Dandadan"),
            titles(LibraryOptions(unread = TriState.ONLY, sort = LibrarySort.LAST_READ, ascending = false)),
        )
    }

    @Test
    fun RF55_filtersCombine() {
        assertEquals(listOf("Akira"), titles(LibraryOptions(completed = TriState.ONLY)))
        assertEquals(listOf("Akira"), titles(LibraryOptions(downloaded = TriState.ONLY)))
        assertEquals(listOf("Dandadan"), titles(LibraryOptions(started = TriState.EXCLUDE)))
        assertEquals(listOf("Dandadan"), titles(LibraryOptions(sources = setOf("local"))))
        assertEquals(listOf("Akira", "Berserk", "One Piece"), titles(LibraryOptions(sources = setOf("mangadex"))))
    }

    @Test
    fun RF55_ongoingCategoriesAndClear() {
        assertEquals(listOf("Berserk", "Dandadan", "One Piece"), titles(LibraryOptions(ongoing = TriState.ONLY)))
        val filtered = LibraryOptions(unread = TriState.ONLY, sort = LibrarySort.UNREAD)
        assertEquals(true, filtered.hasFilters)
        assertEquals(LibraryOptions(sort = LibrarySort.UNREAD), filtered.withoutFilters())
    }

    @Test
    fun RF55_ongoingOrCompletedShowsBoth() {
        assertEquals(
            listOf("Akira", "Berserk", "Dandadan", "One Piece"),
            titles(LibraryOptions(ongoing = TriState.ONLY, completed = TriState.ONLY)),
        )
        assertEquals(listOf("Berserk", "Dandadan", "One Piece"), titles(LibraryOptions(completed = TriState.EXCLUDE)))
    }

    @Test
    fun RF55_everySortOrder() {
        assertEquals(listOf("Akira", "Berserk", "Dandadan", "One Piece"), titles(LibraryOptions()))
        assertEquals(
            listOf("Berserk", "Akira", "Dandadan", "One Piece"),
            titles(LibraryOptions(sort = LibrarySort.LATEST_CHAPTER, ascending = false)),
        )
        assertEquals(
            listOf("Dandadan", "One Piece", "Berserk", "Akira"),
            titles(LibraryOptions(sort = LibrarySort.UNREAD, ascending = false)),
        )
        assertEquals(
            listOf("Dandadan", "Berserk", "Akira", "One Piece"),
            titles(LibraryOptions(sort = LibrarySort.DATE_ADDED, ascending = false)),
        )
        assertEquals(listOf("Akira", "Berserk", "Dandadan", "One Piece"), titles(LibraryOptions(sort = LibrarySort.TOTAL_CHAPTERS)))
    }

    @Test
    fun RF56_searchByTitleAuthorAndGenre() {
        assertEquals(listOf("One Piece"), titles(LibraryOptions(), "oda"))
        assertEquals(listOf("One Piece"), titles(LibraryOptions(), "aventura"))
        assertEquals(listOf("Berserk"), titles(LibraryOptions(), " BERS "))
    }
}
