@file:Suppress("UnusedParameter") // The JavaScript bodies read the parameters by name.

package com.tiagohs.hqr.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import coil3.Bitmap
import com.tiagohs.hqr.presentation.reader.BorderCrop
import org.jetbrains.skia.IRect

private fun requestWakeLock(): JsAny? =
    js(
        """(navigator.wakeLock ? navigator.wakeLock.request('screen').catch(function () { return null; }) : null)""",
    )

private fun releaseWakeLock(lock: JsAny?): Unit = js("""{ if (lock) lock.then(function (l) { if (l) l.release(); }); }""")

@Composable
actual fun KeepScreenOn(on: Boolean) {
    DisposableEffect(on) {
        val lock = if (on) requestWakeLock() else null
        onDispose { releaseWakeLock(lock) }
    }
}

@Composable
@Suppress("UnusedParameter") // The browser has no system bars to hide; full screen is the F key (RF-112).
actual fun ImmersiveMode(hideBars: Boolean) = Unit

internal actual fun Bitmap.widthPx(): Int = width

internal actual fun Bitmap.heightPx(): Int = height

internal actual fun Bitmap.argbAt(
    x: Int,
    y: Int,
): Int = getColor(x, y)

internal actual fun Bitmap.cropTo(area: BorderCrop.Area): Bitmap {
    val out = Bitmap()
    return if (extractSubset(out, IRect.makeXYWH(area.left, area.top, area.width, area.height))) out else this
}
