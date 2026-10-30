package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.ui.navigation.HqrApp
import io.kotest.matchers.shouldBe
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test

/** Phase 5 through the app: downloading from the work screen and the downloads screen (RF-62 to RF-66). */
@OptIn(ExperimentalTestApi::class)
class DownloadsScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(
        matcher: SemanticsMatcher,
        timeout: Long = 10_000,
    ) {
        runCatching { waitUntil(timeoutMillis = timeout) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    @Test
    fun RF62_RF66_downloadNextUnreadAndSelectedThenDelete() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasText("Kaiju Express"))[0].performClick()
            waitFor(hasText("Read"))
            // RF-62: the next 5 unread from the top bar.
            onAllNodes(hasContentDescription("Download"))[0].performClick()
            waitFor(hasText("Next 5 unread"))
            onAllNodes(hasText("Next 5 unread"))[0].performClick()
            // RF-32, RF-62: a long press selects; the selection downloads together.
            onAllNodes(hasText("Capítulo 15"))[0].performTouchInput { longClick() }
            waitFor(hasText("1 selected"))
            screenshot("demo-SCR-OBR-001-selection")
            onAllNodes(hasContentDescription("Download"))[0].performClick()
            // Six chapters of drawn pages go first: a slow machine needs time.
            waitFor(hasContentDescription("Downloaded"), timeout = 90_000)
            // One screen at a time: each back waits for the screen under it.
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Popular"))
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Manage sources and servers"))
            onAllNodes(hasText("Library"))[0].performClick()
            waitFor(hasContentDescription("Downloads"))
            onAllNodes(hasContentDescription("Downloads"))[0].performClick()
            waitFor(hasText("Downloaded"), timeout = 30_000)
            waitUntil(timeoutMillis = 30_000) { onAllNodes(hasText("Kaiju Express")).fetchSemanticsNodes().isNotEmpty() }
            screenshot("demo-SCR-DL-001")
            // RF-66: the whole work's files go at once.
            onAllNodes(hasContentDescription("Delete the downloads of Kaiju Express"))[0].performClick()
            waitFor(hasText("Nothing downloaded"))
        }

    @Test
    fun RF67_RF68_lowSpacePausesWithTheReasonAndCbzIsAnOption() =
        screenTest {
            // 100 MB free: the queue pauses before the first page.
            startTestKoin(extra = module { single<FreeSpace> { FreeSpace { 100L * 1024 * 1024 } } })
            val prefs = GlobalContext.get().get<AppPreferences>()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Kaiju Express"))
            onAllNodes(hasText("Kaiju Express"))[0].performClick()
            waitFor(hasText("Read"))
            onAllNodes(hasContentDescription("Download"))[0].performClick()
            waitFor(hasText("Next 5 unread"))
            onAllNodes(hasText("Next 5 unread"))[0].performClick()
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Popular"))
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Manage sources and servers"))
            onAllNodes(hasText("Library"))[0].performClick()
            waitFor(hasContentDescription("Downloads"))
            onAllNodes(hasContentDescription("Downloads"))[0].performClick()
            waitFor(hasText("Low space: the queue was paused. Free up at least 200 MB and resume."), timeout = 30_000)
            waitFor(hasText("Low space"))
            screenshot("demo-SCR-DL-001-low-space")
            onAllNodes(hasText("Save as CBZ"))[0].performClick()
            prefs.downloadsAsCbz.value shouldBe true
        }

    @Test
    fun RF72_RF73_backupFromSettingsAndRestoreShowsTheSummary() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Settings"))
            onAllNodes(hasText("Settings"))[0].performClick()
            waitFor(hasText("Backup and restore"))
            onAllNodes(hasText("Backup and restore"))[0].performScrollTo().performClick()
            waitFor(hasText("Create backup"))
            onAllNodes(hasText("Create backup"))[0].performClick()
            waitFor(hasText("Backup saved."))
            screenshot("demo-SCR-SET-002")
            onAllNodes(hasText("Restore backup"))[0].performClick()
            waitFor(hasText("Backup restored"))
        }
}
