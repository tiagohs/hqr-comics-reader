package com.tiagohs.hqr.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToLog
import coil3.size.Size
import com.tiagohs.hqr.ui.navigation.HqrApp
import com.tiagohs.hqr.ui.reader.CropBordersTransformation
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The reader's layout on screen (RF-44, RF-45). */
@OptIn(ExperimentalTestApi::class)
class ReaderScreensTest {
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
    fun RF44_landscapeShowsPagesTwoAndThreeTogether() =
        screenTest(width = 1280, height = 800) {
            startTestKoin()
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
            onRoot().performKeyInput { pressKey(Key.Spacebar) }
            waitFor(hasContentDescription("3 of 22"))
            waitUntil(timeoutMillis = 10_000) {
                runCatching {
                    onAllNodes(hasContentDescription("2 of 22"))[0].assertIsDisplayed()
                    onAllNodes(hasContentDescription("3 of 22"))[0].assertIsDisplayed()
                }.isSuccess
            }
            screenshot("demo-SCR-RDR-001-spread")
        }

    @Test
    fun RF45_cropBordersCutsTheWhiteMarginOfAPage() {
        val bitmap = Bitmap().apply { allocN32Pixels(200, 300) }
        Canvas(bitmap).apply {
            clear(Color.WHITE)
            drawRect(Rect.makeXYWH(20f, 30f, 160f, 240f), Paint().apply { color = Color.makeRGB(40, 60, 120) })
        }
        val cropped = runBlocking { CropBordersTransformation.transform(bitmap, Size.ORIGINAL) }
        assertTrue(cropped.width in 160..164, "width ${cropped.width}")
        assertTrue(cropped.height in 240..244, "height ${cropped.height}")
        // A page with no margin comes back as it was.
        val full = Bitmap().apply { allocN32Pixels(50, 50) }
        Canvas(full).clear(Color.makeRGB(40, 60, 120))
        assertEquals(50, runBlocking { CropBordersTransformation.transform(full, Size.ORIGINAL) }.width)
    }
}
