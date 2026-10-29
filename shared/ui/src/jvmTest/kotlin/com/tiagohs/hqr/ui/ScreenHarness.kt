package com.tiagohs.hqr.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.ui.di.BuiltInSources
import com.tiagohs.hqr.ui.di.coreModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

/** Starts Koin with an in-memory database, the demo source and map settings, like the apps do. */
fun startTestKoin(demo: DemoSource = DemoSource()): HqrDatabase {
    val db = inMemoryDatabase()
    startKoin {
        modules(
            module {
                single<HqrDatabase> { db }
                single<Settings> { PropertiesSettings(java.util.Properties()) }
                single<HttpClient> { hqrHttpClient("HQR-test") }
                single<ImageCache> { MemoryImageCache() }
                single<List<Source>>(BuiltInSources) { listOf(demo) }
            },
            coreModule,
        )
    }
    return db
}

fun stopTestKoin() = stopKoin()

/** Saves the current screen to build/screenshots/<name>.png for visual review against the canvas. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.screenshot(name: String) {
    waitForIdle()
    val bitmap = onRoot().captureToImage().asSkiaBitmap()
    val data = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG) ?: return
    val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
    File(dir, "$name.png").writeBytes(data.bytes)
}

@OptIn(ExperimentalTestApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun screenTest(
    width: Int = PHONE_WIDTH,
    height: Int = PHONE_HEIGHT,
    block: ComposeUiTest.() -> Unit,
) {
    // The test drives input from its own thread; an unconfined Main makes lifecycle and ViewModels accept it.
    kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
    try {
        runScreen(width, height, block)
    } finally {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalTestApi::class)
private fun runScreen(
    width: Int,
    height: Int,
    block: ComposeUiTest.() -> Unit,
) {
    var failure: Throwable? = null
    runCatching {
        runDesktopComposeUiTest(width, height) {
            try {
                block()
            } catch (e: Throwable) {
                failure = e
                runCatching { screenshot("failure") }
            }
        }
    }.onFailure { if (failure == null) throw it }
    failure?.let { throw it }
}

/** Window sizes in pixels at density 1: a phone (compact) and a landscape tablet (expanded). */
const val PHONE_WIDTH = 412
const val PHONE_HEIGHT = 892
const val TABLET_WIDTH = 1280
const val TABLET_HEIGHT = 800
