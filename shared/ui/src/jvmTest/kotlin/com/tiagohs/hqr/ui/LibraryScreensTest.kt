package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test

/** Phase 2 through the app: long press to add, categories, and the filter, sort and display sheet. */
@OptIn(ExperimentalTestApi::class)
class LibraryScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 5_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    private fun ComposeUiTest.tap(text: String) = onAllNodes(hasText(text))[0].performClick()

    @Test
    fun RF52_RF53_RF54_libraryCategoriesAndDisplay() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            tap("Explore")
            waitFor(hasText("MangaDex"))
            tap("MangaDex")
            waitFor(hasText("Kaiju Express"))
            // Long press adds to the library (RF-52).
            listOf("Kaiju Express", "Céu de Papel", "Moça do Farol").forEach { title ->
                onAllNodes(hasText(title))[0].performTouchInput { longClick() }
                waitFor(hasText("Added to the library"))
                mainClock.advanceTimeBy(5_000)
            }
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Sources"))
            mainClock.advanceTimeBy(1_000)
            tap("Library")
            runCatching { waitFor(hasText("Céu de Papel")) }.onFailure {
                onRoot().printToLog("LIB")
                throw it
            }
            screenshot("demo-SCR-LIB-001-light")
            // Categories (RF-53).
            onAllNodes(hasContentDescription("More options"))[0].performClick()
            tap("Categories")
            waitFor(hasContentDescription("New category"))
            onAllNodes(hasContentDescription("New category"))[0].performClick()
            runCatching { waitFor(hasSetTextAction()) }.onFailure {
                println(
                    "ROOTS " +
                        onAllNodes(
                            androidx.compose.ui.test
                                .isRoot(),
                        ).fetchSemanticsNodes().size,
                )
                onAllNodes(
                    androidx.compose.ui.test
                        .isRoot(),
                ).fetchSemanticsNodes().forEach { println("ROOT " + it.config) }
                throw it
            }
            onAllNodes(hasSetTextAction())[0].performTextInput("Lendo")
            tap("Save")
            waitFor(hasText("Lendo"))
            screenshot("demo-SCR-LIB-003-light")
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Lendo 0"))
            // Options sheet: list display (RF-54).
            onAllNodes(hasContentDescription("Filter, sort and display"))[0].performClick()
            waitFor(hasText("Show 3 works"))
            tap("Display")
            tap("List")
            screenshot("demo-SCR-LIB-002-light")
            tap("Show 3 works")
            waitFor(hasText("Moça do Farol"))
            screenshot("demo-SCR-LIB-001-list-light")
        }
}
