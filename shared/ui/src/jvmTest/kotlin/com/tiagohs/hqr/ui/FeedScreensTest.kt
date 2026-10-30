package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test

/** Saved searches, the feed and recent searches through the app (RF-24, RF-25, RF-27). */
@OptIn(ExperimentalTestApi::class)
class FeedScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    @Test
    fun RF24_RF25_saveASearchPutItInTheFeedAndSeeTheWorks() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasSetTextAction())[0].performTextInput("kaiju")
            onAllNodes(hasSetTextAction())[0].performImeAction()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasContentDescription("More options"))[0].performClick()
            waitFor(hasText("Save this search"))
            onAllNodes(hasText("Save this search"))[0].performClick()
            waitFor(hasText("Name"))
            onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
            onAllNodes(hasSetTextAction())[1].performTextReplacement("Monstros")
            onAllNodes(hasText("Save"))[0].performClick()
            waitFor(hasText("Monstros"))
            screenshot("demo-SCR-EXP-002-saved")
            onAllNodes(hasContentDescription("Options for Monstros"))[0].performClick()
            waitFor(hasText("Add to feed"))
            onAllNodes(hasText("Add to feed"))[0].performClick()
            waitFor(hasText("Added to the feed"))
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Feed"))
            onAllNodes(hasText("Feed"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            waitFor(hasText("Monstros"))
            screenshot("demo-SCR-EXP-001-feed")
        }

    @Test
    fun RF27_recentSearchesShowWhenTheFieldIsEmpty() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasContentDescription("Search all sources"))
            onAllNodes(hasContentDescription("Search all sources"))[0].performClick()
            waitFor(hasSetTextAction())
            onAllNodes(hasSetTextAction())[0].performTextInput("kaiju")
            onAllNodes(hasSetTextAction())[0].performImeAction()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasSetTextAction())[0].performTextReplacement("")
            waitFor(hasText("Recent searches"))
            waitFor(hasContentDescription("Remove kaiju"))
            onAllNodes(hasContentDescription("Remove kaiju"))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Recent searches")).fetchSemanticsNodes().isEmpty() }
        }
}
