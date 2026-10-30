package com.tiagohs.hqr.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.net.ImageCache
import com.tiagohs.hqr.data.net.MemoryImageCache
import com.tiagohs.hqr.data.net.hqrHttpClient
import com.tiagohs.hqr.data.settings.SettingsSecretStore
import com.tiagohs.hqr.data.sources.SourceListFetcher
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.ui.di.AppVersion
import com.tiagohs.hqr.ui.di.BuiltInSources
import com.tiagohs.hqr.ui.di.coreModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

/** Starts Koin with an in-memory database, the given sources and map settings, like the apps do. */
fun startTestKoin(sources: (HttpClient) -> List<Source> = { listOf(DemoSource()) }): HqrDatabase {
    val db = inMemoryDatabase()
    startKoin {
        modules(
            module {
                single<HqrDatabase> { db }
                single<Settings> { PropertiesSettings(java.util.Properties()) }
                single<HttpClient> { hqrHttpClient("HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)") }
                single<ImageCache> { MemoryImageCache() }
                single<List<Source>>(BuiltInSources) { sources(get()) }
                single<SourceListFetcher> { SourceListFetcher { null } }
                single<SecretStore> { SettingsSecretStore(get()) }
                single(AppVersion) { "1.0.0" }
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
    // Dialogs and sheets are roots of their own: the picture is every root drawn in order, as on screen.
    val roots = onAllNodes(isRoot()).fetchSemanticsNodes()
    val images = roots.indices.map { onAllNodes(isRoot())[it].captureToImage().asSkiaBitmap() }
    val first = images.first()
    val surface = Surface.makeRasterN32Premul(first.width, first.height)
    roots.forEachIndexed { i, root ->
        val bounds = root.boundsInRoot
        surface.canvas.drawImage(Image.makeFromBitmap(images[i]), bounds.left, bounds.top)
    }
    val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG) ?: return
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
