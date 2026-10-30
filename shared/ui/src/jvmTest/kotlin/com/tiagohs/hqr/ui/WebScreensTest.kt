package com.tiagohs.hqr.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.BridgeCheck
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test

/** The web milestone's screens, run on the JVM with the browser pieces faked (RF-108, RF-112). */
@OptIn(ExperimentalTestApi::class)
class WebScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    /** A bridge that accepts only the token "certo". */
    private class FakeBridge : Bridge {
        override val address = MutableStateFlow<String?>(null)

        override fun needs(id: SourceId) = id.value == "demo"

        override suspend fun test(
            address: String,
            token: String,
        ) = if (token == "certo") BridgeCheck.OK else BridgeCheck.TOKEN_REFUSED

        override suspend fun save(
            address: String,
            token: String,
        ) {
            this.address.value = address
        }

        override suspend fun clear() {
            address.value = null
        }
    }

    @Test
    fun RF108_withoutABridgeTheSourceAsksForItAndTheTestSaysTheCause() =
        screenTest {
            val bridge = FakeBridge()
            startTestKoin(extra = module { single<Bridge> { bridge } })
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Needs the bridge"))
            screenshot("demo-SCR-EXP-001-needs-bridge")
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Bridge address"))
            onAllNodes(hasSetTextAction())[0].performTextInput("https://hqr.tiago.workers.dev")
            onAllNodes(hasSetTextAction())[1].performTextInput("errado")
            onAllNodes(hasText("Test"))[0].performClick()
            waitFor(hasText("Token refused by the bridge"))
            screenshot("demo-SCR-WEB-001")
            onAllNodes(hasSetTextAction())[1].performTextReplacement("certo")
            onAllNodes(hasText("Test"))[0].performClick()
            waitFor(hasText("The bridge answered. Online sources are ready."))
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Needs the bridge")).fetchSemanticsNodes().isEmpty() }
        }

    @Test
    fun RF112_keyboardTurnsPagesAndQuestionMarkShowsTheShortcuts() =
        screenTest {
            val db = startTestKoin()
            setAppContent { HqrApp(systemDark = true) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasText("Kaiju Express"))[0].performClick()
            waitFor(hasText("Read"))
            onAllNodes(hasText("Read"))[0].performClick()
            waitFor(hasContentDescription("1 of 22"))
            // A manga reads right to left: Space always goes forward, and so does the left arrow here.
            repeat(2) { onRoot().performKeyInput { pressKey(Key.Spacebar) } }
            onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
            // Three pages forward: the reader saves page 4 (index 3).
            waitUntil(timeoutMillis = 10_000) { runBlocking { db.progressDao().listForManga(1).any { it.page == 3 } } }
            onRoot().performKeyInput {
                keyDown(Key.ShiftLeft)
                pressKey(Key.Slash)
                keyUp(Key.ShiftLeft)
            }
            waitFor(hasText("Keyboard shortcuts"))
            screenshot("demo-SCR-WEB-003-shortcuts")
        }
}
