package com.tiagohs.hqr.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.ui.navigation.HqrApp
import com.tiagohs.hqr.ui.theme.HqrTheme
import com.tiagohs.hqr.ui.theme.LocalDynamicColor
import com.tiagohs.hqr.ui.theme.LocalPureBlack
import org.koin.core.context.GlobalContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Phase 6 through the app: theme, language and the About screen (RF-76 to RF-78). */
@OptIn(ExperimentalTestApi::class)
class SettingsScreensTest {
    @AfterTest
    fun tearDown() {
        runCatching { GlobalContext.get().get<AppPreferences>().setAppLanguage(null) }
        stopTestKoin()
    }

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    @Test
    fun RF76_pureBlackMakesTheDarkBackgroundBlackWithOrWithoutSystemColors() =
        screenTest {
            val backgrounds = mutableMapOf<String, Color>()
            setContent {
                listOf(false, true).forEach { black ->
                    CompositionLocalProvider(LocalPureBlack provides black, LocalDynamicColor provides true) {
                        HqrTheme(dark = true, animate = false) { backgrounds["dark $black"] = MaterialTheme.colorScheme.background }
                        HqrTheme(dark = false, animate = false) { backgrounds["light $black"] = MaterialTheme.colorScheme.background }
                    }
                }
            }
            waitForIdle()
            assertEquals(Color.Black, backgrounds["dark true"])
            assertNotEquals(Color.Black, backgrounds["dark false"])
            // Light stays light: "preto puro" is for the dark theme only.
            assertNotEquals(Color.Black, backgrounds["light true"])
        }

    @Test
    fun RF77_languageChosenInTheAppThenBackToTheDevice() =
        screenTest {
            startTestKoin()
            val prefs = GlobalContext.get().get<AppPreferences>()
            prefs.setAppLanguage("es")
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Ajustes"))
            waitFor(hasText("Tu biblioteca está vacía"))
            prefs.setAppLanguage("pt")
            waitFor(hasText("Sua biblioteca está vazia"))
            prefs.setAppLanguage(null)
            waitFor(hasText("Your library is empty"))
        }

    @Test
    fun RF78_aboutListsMangaDexLicensesAndTheNotice() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Settings"))
            onAllNodes(hasText("Settings"))[0].performClick()
            waitFor(hasScrollAction())
            onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("About HQR"))
            onAllNodes(hasText("About HQR"))[0].performClick()
            waitFor(hasText("Data and images from MangaDex", substring = true))
            waitFor(hasText("HQR hosts no content", substring = true))
            waitFor(hasText("NOTICE", substring = true))
            screenshot("demo-SCR-SET-003")
            onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Open source licenses"))
            onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Full privacy policy"))
            waitFor(hasText("Apache License 2.0"))
        }
}
