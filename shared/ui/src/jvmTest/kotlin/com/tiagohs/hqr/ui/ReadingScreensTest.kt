package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test

/** Reading marked by hand, the add button on the results and the library areas (RF-126 to RF-129). */
@OptIn(ExperimentalTestApi::class)
class ReadingScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    private fun ComposeUiTest.tap(text: String) = onAllNodes(hasText(text))[0].performClick()

    private fun ComposeUiTest.openSource() {
        waitFor(hasText("Explore"))
        tap("Explore")
        waitFor(hasText("MangaDex"))
        tap("MangaDex")
        waitFor(hasText("Kaiju Express"))
    }

    @Test
    fun RF127_RF126_the_cover_button_adds_and_the_message_opens_mark_reading() =
        screenTest {
            val db = startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            openSource()
            onAllNodes(hasContentDescription("Add Kaiju Express to the library"))[0].performClick()
            waitFor(hasText("Kaiju Express is in the library"))
            waitFor(hasContentDescription("Kaiju Express is in the library. Remove"))
            screenshot("demo-SCR-EXP-002-add")
            tap("Mark reading")
            // The work never opened: the sheet asks the source for the chapters first.
            waitFor(hasText("I read up to a chapter"))
            waitFor(hasText("Kaiju Express · 15 chapters", substring = true))
            tap("I read up to a chapter")
            repeat(3) { onAllNodes(hasContentDescription("Next chapter"))[0].performClick() }
            screenshot("demo-SCR-LIB-004-sheet")
            tap("Save")
            waitFor(hasText("Kaiju Express: read up to ch.", substring = true))
            val manga = runBlocking { db.mangaDao().find("demo", "m1")!! }
            waitUntil(timeoutMillis = 10_000) {
                runBlocking { db.progressDao().listForManga(manga.id).count { it.read } } > 0
            }
        }

    @Test
    fun RF126_RF129_one_work_marked_from_the_library_shows_its_reading_on_the_cover() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            openSource()
            onAllNodes(hasText("Céu de Papel"))[0].performTouchInput { longClick() }
            waitFor(hasText("Céu de Papel is in the library"))
            mainClock.advanceTimeBy(5_000)
            onAllNodes(hasContentDescription("Add Kaiju Express to the library"))[0].performClick()
            waitFor(hasText("Kaiju Express is in the library"))
            mainClock.advanceTimeBy(5_000)
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Sources"))
            tap("Library")
            waitFor(hasText("Céu de Papel"))
            onAllNodes(hasText("Céu de Papel"))[0].performTouchInput { longClick() }
            onAllNodes(hasContentDescription("Mark reading"))[0].performClick()
            waitFor(hasText("I read up to a chapter"))
            tap("I read up to a chapter")
            tap("Save")
            waitFor(hasText("Céu de Papel: read up to ch.", substring = true))
            waitFor(hasContentDescription("of 15 read", substring = true))
            screenshot("demo-SCR-LIB-001-progress")
            // Several works: only all or none.
            onAllNodes(hasText("Céu de Papel"))[0].performTouchInput { longClick() }
            onAllNodes(hasContentDescription("Select all"))[0].performClick()
            onAllNodes(hasContentDescription("Mark reading"))[0].performClick()
            waitFor(hasText("I read all of them"))
            tap("I read all of them")
            waitFor(hasText("works marked as read", substring = true))
        }

    @Test
    fun RF128_the_library_splits_comics_and_manga_and_a_work_can_move() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = true) }
            openSource()
            onAllNodes(hasContentDescription("Add Kaiju Express to the library"))[0].performClick()
            waitFor(hasText("Kaiju Express is in the library"))
            mainClock.advanceTimeBy(5_000)
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Sources"))
            tap("Library")
            // The demo source is not a manga source: its works start in Comics.
            waitFor(hasText("Comics 1"))
            waitFor(hasText("Manga 0"))
            tap("Kaiju Express")
            waitFor(hasText("Read ch.", substring = true))
            // The work's menu is the one in the top bar; each chapter has its own below.
            val menus = onAllNodes(hasContentDescription("More options"))
            menus[
                menus
                    .fetchSemanticsNodes()
                    .withIndex()
                    .minBy { it.value.boundsInRoot.top }
                    .index,
            ].performClick()
            tap("Where it goes in the library")
            waitFor(hasText("Like the source"))
            screenshot("demo-SCR-OBR-001-shelf-dark")
            tap("Manga")
            tap("OK")
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Manga 1"))
            tap("Manga 1")
            waitFor(hasText("Kaiju Express"))
            screenshot("demo-SCR-LIB-001-manga-dark")
        }
}
