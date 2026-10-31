package com.tiagohs.hqr.presentation.library

import com.tiagohs.hqr.domain.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Changing the library options (SCR-LIB-002), the same rules on every platform. */
class LibraryOptionsTest {
    @Test
    fun RF55_aToZStartsAscendingTheOthersStartWithTheMostAndTheSameOrderKeepsItsDirection() {
        val title = LibraryOptions().withSort(LibrarySort.UNREAD)
        assertEquals(LibrarySort.UNREAD, title.sort)
        assertFalse(title.ascending)
        assertTrue(title.withSort(LibrarySort.TITLE).ascending)
        assertTrue(LibraryOptions(sort = LibrarySort.UNREAD, ascending = true).withSort(LibrarySort.UNREAD).ascending)
        assertTrue(LibraryOptions().withAscending(false).withAscending(true).ascending)
    }

    @Test
    fun RF55_aStateFilterTurnsOnAndOff() {
        val on = LibraryOptions().toggle(LibraryOptions.StateFilter.UNREAD)
        assertTrue(on.isOn(LibraryOptions.StateFilter.UNREAD))
        assertEquals(TriState.ONLY, on.unread)
        assertFalse(on.toggle(LibraryOptions.StateFilter.UNREAD).isOn(LibraryOptions.StateFilter.UNREAD))
        assertTrue(LibraryOptions().toggle(LibraryOptions.StateFilter.COMPLETED).isOn(LibraryOptions.StateFilter.COMPLETED))
    }

    @Test
    fun RF55_sourcesAndCategoriesAreAddedAndTakenOut() {
        val options = LibraryOptions().toggleSource("mangadex").toggleCategory(Category.DEFAULT_ID).toggleCategory(3)
        assertEquals(setOf("mangadex"), options.sources)
        assertEquals(setOf(Category.DEFAULT_ID, 3L), options.categories)
        assertEquals(emptySet(), options.toggleSource("mangadex").sources)
        assertTrue(options.hasFilters)
    }

    @Test
    fun RF54_displayAndBadgesChange() {
        val options = LibraryOptions().withDisplay(LibraryDisplay.LIST).withBadge(LibraryOptions.Badge.SOURCE, true)
        assertEquals(LibraryDisplay.LIST, options.display)
        assertTrue(options.badgeOn(LibraryOptions.Badge.SOURCE))
        assertFalse(options.withBadge(LibraryOptions.Badge.UNREAD, false).badgeOn(LibraryOptions.Badge.UNREAD))
    }
}
