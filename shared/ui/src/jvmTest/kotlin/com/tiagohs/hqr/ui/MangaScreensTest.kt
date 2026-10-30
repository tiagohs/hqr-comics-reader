package com.tiagohs.hqr.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The work screen's ways out to the web (RF-11, RF-34). */
@OptIn(ExperimentalTestApi::class)
class MangaScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private val opened = mutableListOf<String>()
    private val uriHandler =
        object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    private fun ComposeUiTest.openWork() {
        setAppContent { CompositionLocalProvider(LocalUriHandler provides uriHandler) { HqrApp(systemDark = false) } }
        waitFor(hasText("Explore"))
        onAllNodes(hasText("Explore"))[0].performClick()
        waitFor(hasText("MangaDex"))
        onAllNodes(hasText("MangaDex"))[0].performClick()
        waitFor(hasText("Kaiju Express"))
        onAllNodes(hasText("Kaiju Express"))[0].performClick()
        waitFor(hasText("Read"))
    }

    @Test
    fun RF11_externalChapterIsMarkedAndOpensThePublisherSite() =
        screenTest {
            startTestKoin { listOf(DemoSource(externalChapter = true)) }
            openWork()
            waitFor(hasText("External"))
            screenshot("demo-SCR-OBR-001-external")
            onAllNodes(hasText("Capítulo 16"))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { opened.isNotEmpty() }
            assertEquals(listOf("https://publisher.example/c16"), opened)
        }

    @Test
    fun RF34_openInBrowserOpensTheWorkPage() =
        screenTest {
            startTestKoin()
            openWork()
            onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Open site", substring = true))
            onAllNodes(hasText("Open site", substring = true))[0].performClick()
            waitUntil(timeoutMillis = 5_000) { opened.isNotEmpty() }
            assertEquals(listOf("https://example.org/m1"), opened)
        }
}
