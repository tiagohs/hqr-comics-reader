package com.tiagohs.hqr.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import com.tiagohs.hqr.domain.source.Bridge
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

    /** The Worker's bridge: ready once the browser is signed in. */
    private class FakeBridge : Bridge {
        override val ready = MutableStateFlow(false)

        override fun needs(id: SourceId) = id.value == "demo"
    }

    @Test
    fun RF108_signedOutTheOnlineSourceAsksToSignIn() =
        screenTest {
            val bridge = FakeBridge()
            val account = FakeAccount()
            startTestKoin(
                extra =
                    module {
                        single<Bridge> { bridge }
                        single<AccountRepository> { account }
                        single<GoogleIdTokens> { GoogleIdTokens { "tiago@example.com" } }
                    },
            )
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Sign in to read online"))
            screenshot("demo-SCR-EXP-001-sign-in")
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Sign in with Google"))
            onAllNodes(hasText("Sign in with Google"))[0].performClick()
            waitFor(hasText("tiago@example.com"))
            bridge.ready.value = true
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Sign in to read online")).fetchSemanticsNodes().isEmpty() }
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
            waitFor(hasText("Read ch.", substring = true))
            onAllNodes(hasText("Read ch.", substring = true))[0].performClick()
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
            // Closing the list gives the keys back to the reader.
            onAllNodes(hasText("OK"))[0].performClick()
            onRoot().performKeyInput { pressKey(Key.Spacebar) }
            waitUntil(timeoutMillis = 10_000) { runBlocking { db.progressDao().listForManga(1).any { it.page == 4 } } }
        }

    @Test
    fun RF114_theBrowserShowsWhatOnlyThePhoneDoesWithTheReason() =
        screenTest {
            startTestKoin()
            setAppContent {
                androidx.compose.runtime.CompositionLocalProvider(com.tiagohs.hqr.ui.settings.LocalRunsInBrowser provides true) {
                    HqrApp(systemDark = false)
                }
            }
            waitFor(hasText("Settings"))
            onAllNodes(hasText("Settings"))[0].performClick()
            waitFor(
                androidx.compose.ui.test
                    .hasScrollAction(),
            )
            onAllNodes(
                androidx.compose.ui.test
                    .hasScrollAction(),
            )[0]
                .performScrollToNode(hasText("The web version sends no crash reports.", substring = true))
            waitFor(hasText("Only in the phone app", substring = true))
        }
}
