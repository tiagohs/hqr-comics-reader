package com.tiagohs.hqr.ui.reader

import androidx.compose.runtime.Composable
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import com.tiagohs.hqr.presentation.reader.BorderCrop

/** RF-46: keeps the screen on while [on]; the browser asks for a wake lock. */
@Composable
expect fun KeepScreenOn(on: Boolean)

/** RF-46: hides the system bars while reading and lets the page use the camera cutout; nothing outside the phone. */
@Composable
expect fun ImmersiveMode(hideBars: Boolean)

internal expect fun Bitmap.widthPx(): Int

internal expect fun Bitmap.heightPx(): Int

/** ARGB at (x, y). */
internal expect fun Bitmap.argbAt(
    x: Int,
    y: Int,
): Int

internal expect fun Bitmap.cropTo(area: BorderCrop.Area): Bitmap

/** RF-45: cuts uniform white or black margins of a page before it is drawn. */
object CropBordersTransformation : Transformation() {
    override val cacheKey: String = "hqr.cropBorders"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val area = BorderCrop.area(input.widthPx(), input.heightPx()) { x, y -> input.argbAt(x, y) } ?: return input
        return input.cropTo(area)
    }
}
