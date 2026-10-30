package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class NavigationTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    @Test
    fun RF88_bottomBarGoesBackToTheLibrary() =
        screenTest {
            startTestKoin()
            setContent { HqrApp(systemDark = false) }
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your library is empty")).fetchSemanticsNodes().isNotEmpty() }
            // The empty library's "Explore" button (index 0), then the Library tab must bring the Library back.
            onAllNodes(hasText("Explore"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Sources")).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasText("Library"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your library is empty")).fetchSemanticsNodes().isNotEmpty() }
            // Into a source and back, then the bottom bar.
            onAllNodes(hasText("Explore"))[1].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("MangaDex")).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Kaiju Express")).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(
                androidx.compose.ui.test
                    .hasContentDescription("Back"),
            )[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Sources")).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasText("Library"))[0].performClick()
            runCatching {
                waitUntil(
                    timeoutMillis = 5_000,
                ) { onAllNodes(hasText("Your library is empty")).fetchSemanticsNodes().isNotEmpty() }
            }.onFailure {
                onRoot().printToLog("NAV")
                throw it
            }
            onAllNodes(hasText("History"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Nothing read yet")).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasText("Library"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your library is empty")).fetchSemanticsNodes().isNotEmpty() }
        }
}
