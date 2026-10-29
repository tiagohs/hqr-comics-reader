package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test

/** Integration through the real app: navigation, screens, ViewModels and the database, with a demo source. */
@OptIn(ExperimentalTestApi::class)
class AppScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    @Test
    fun FEAT001_exploreToWorkToReader() =
        screenTest {
            startTestKoin()
            setContent { HqrApp(systemDark = true) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Sua biblioteca está vazia").fetchSemanticsNodes().isNotEmpty() ||
                    onAllNodesWithText("Your library is empty").fetchSemanticsNodes().isNotEmpty()
            }
            screenshot("SCR-LIB-001-empty-dark")
            onAllNodesWithText("Explorar").fetchSemanticsNodes().let {
                if (it.isNotEmpty()) onAllNodesWithText("Explorar")[0].performClick() else onAllNodesWithText("Explore")[0].performClick()
            }
            runCatching {
                waitUntil(
                    timeoutMillis = 5_000,
                ) { onAllNodesWithText("MangaDex").fetchSemanticsNodes().isNotEmpty() }
            }.onFailure {
                onRoot().printToLog("TREE")
                screenshot("debug")
            }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("MangaDex").fetchSemanticsNodes().isNotEmpty() }
            screenshot("SCR-EXP-001-dark")
            onNodeWithText("MangaDex").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Kaiju Express").fetchSemanticsNodes().isNotEmpty() }
            screenshot("SCR-EXP-002-dark")
            onAllNodesWithText("Lâmina do Norte")[0].performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Capítulo 15")).fetchSemanticsNodes().isNotEmpty() }
            screenshot("SCR-OBR-001-dark")
        }
}
