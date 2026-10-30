package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.presentation.color.CoverColor
import com.tiagohs.hqr.sources.mangadex.MangaDexSource
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DOUBLE_TAP_WINDOW_MS = 600L

/** Integration through the real app: navigation, screens, ViewModels and the database. */
@OptIn(ExperimentalTestApi::class)
class AppScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(
        matcher: SemanticsMatcher,
        timeout: Long = 5_000,
    ) = waitUntil(timeoutMillis = timeout) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    /** FEAT-001 flow: Explore → source → Popular → work → Read → turn a page → back. Returns the saved page. */
    private fun ComposeUiTest.readFlow(
        db: HqrDatabase,
        prefix: String,
        firstWork: () -> String,
        timeout: Long,
    ): Int {
        setAppContent { HqrApp(systemDark = true) }
        waitFor(hasText("Your library is empty"), timeout)
        screenshot("$prefix-SCR-LIB-001-empty")
        onAllNodes(hasText("Explore"))[0].performClick()
        waitFor(hasText("MangaDex"), timeout)
        screenshot("$prefix-SCR-EXP-001")
        onAllNodes(hasText("MangaDex"))[0].performClick()
        val title = firstWork()
        waitFor(hasText(title), timeout)
        Thread.sleep(if (prefix == "live") 3_000 else 0)
        screenshot("$prefix-SCR-EXP-002")
        onAllNodes(hasText(title))[0].performClick()
        waitFor(hasText("Read"), timeout)
        Thread.sleep(if (prefix == "live") 2_000 else 0)
        screenshot("$prefix-SCR-OBR-001")
        onAllNodes(hasText("Read"))[0].performClick()
        waitFor(hasContentDescription("1 of ", substring = true), timeout)
        Thread.sleep(if (prefix == "live") 4_000 else 300)
        screenshot("$prefix-SCR-RDR-001")
        if (prefix == "demo") {
            // Right to left: the left edge moves forward (RF-38). Live works may open in webtoon mode (RF-37).
            onRoot().performTouchInput { click(centerLeft.copy(x = 10f)) }
            // Past the double tap window (RF-39 zoom), so the next tap is a single one.
            mainClock.advanceTimeBy(DOUBLE_TAP_WINDOW_MS)
        }
        onRoot().performTouchInput { click(center.copy(y = center.y * 0.9f)) }
        mainClock.advanceTimeBy(DOUBLE_TAP_WINDOW_MS)
        waitFor(if (prefix == "demo") hasText("2 of 22") else hasText(" of ", substring = true), timeout)
        screenshot("$prefix-SCR-RDR-001-menu")
        onAllNodes(hasContentDescription("Back"))[0].performClick()
        // Demo: page 2 of the chapter, so "Continue"; live (webtoon, not scrolled): still "Read" (RF-31).
        waitFor(if (prefix == "demo") hasText("Continue", substring = true) else hasText("Read"), timeout)
        screenshot("$prefix-SCR-OBR-001-continue")
        // Leaving the reader saves at once (RF-43); the write runs on Room's threads.
        repeat(30) {
            val page = runBlocking { db.progressDao().listForManga(1).maxOfOrNull { it.page } }
            if (page == 1 || (prefix == "live" && page != null)) return page
            Thread.sleep(100)
        }
        return runBlocking { db.progressDao().listForManga(1).maxOfOrNull { it.page } ?: -1 }
    }

    @Test
    fun FEAT001_RF43_exploreToWorkToReaderSavesThePage() =
        screenTest {
            val db = startTestKoin()
            assertEquals(1, readFlow(db, "demo", { "Kaiju Express" }, 5_000))
            // RF-98: the green cover gave the work its own color (decoded on the JVM like on the devices).
            val color = runBlocking { db.mangaDao().get(1)?.coverColor }
            assertTrue(color != null && color != CoverColor.VIOLET, "cover color: $color")
        }

    @Test
    fun RF43_webtoonEndMarksTheChapterReadAndShowsTheTransition() =
        screenTest {
            val db = startTestKoin { listOf(DemoSource(pageCount = 3, kind = ReadingKind.MANHWA)) }
            setAppContent { HqrApp(systemDark = true) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasText("Kaiju Express"))[0].performClick()
            waitFor(hasText("Read"))
            onAllNodes(hasText("Read"))[0].performClick()
            waitFor(hasContentDescription("1 of 3"))
            onNode(hasScrollAction()).performScrollToIndex(2)
            waitFor(hasText("You finished"))
            screenshot("demo-SCR-RDR-003-webtoon-end")
            repeat(30) {
                if (runBlocking { db.progressDao().listForManga(1).any { it.read } }) return@screenTest
                Thread.sleep(100)
            }
            error("the chapter was not marked read")
        }

    @Test
    fun FEAT001_liveMangaDex() {
        if (System.getProperty("liveCheck") != "true") return
        screenTest {
            val db = startTestKoin { http -> listOf<Source>(MangaDexSource(http)) }
            val probe =
                MangaDexSource(
                    com.tiagohs.hqr.data.net
                        .hqrHttpClient("HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)"),
                )
            val first =
                runBlocking {
                    probe
                        .popular(1)
                        .items
                        .first()
                        .title
                }
            readFlow(db, "live", { first }, 30_000)
        }
    }
}
