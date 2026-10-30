package com.tiagohs.hqr.ui.reader

import androidx.compose.runtime.Composable
import coil3.Bitmap
import com.tiagohs.hqr.presentation.reader.BorderCrop
import org.jetbrains.skia.IRect

@Composable
@Suppress("UnusedParameter") // Same signature as the phone's.
actual fun KeepScreenOn(on: Boolean) = Unit

@Composable
@Suppress("UnusedParameter") // Same signature as the phone's.
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
