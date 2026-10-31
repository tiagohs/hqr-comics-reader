@file:OptIn(ExperimentalForeignApi::class)

package com.tiagohs.hqr.app

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.presentation.color.CoverPixels
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

/** RF-98 on iOS: the cover read through the image cache and drawn into at most [MAX_SIDE]² RGBA pixels. */
class IosCoverPixels(
    private val images: ImageBytes,
) : CoverPixels {
    override suspend fun pixels(manga: Manga): IntArray? {
        val url = manga.source.coverUrl ?: return null
        val bytes = images.load(manga.sourceId, url, emptyMap())
        return withContext(Dispatchers.Default) { argb(bytes) }
    }

    @Suppress("MagicNumber") // RGBA bytes into ARGB ints.
    internal fun argb(bytes: ByteArray): IntArray? {
        if (bytes.isEmpty()) return null
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
        val image = UIImage.imageWithData(data)?.CGImage ?: return null
        val width = CGImageGetWidth(image).toInt()
        val height = CGImageGetHeight(image).toInt()
        if (width == 0 || height == 0) return null
        val step = maxOf(1, maxOf(width, height) / MAX_SIDE)
        val columns = (width + step - 1) / step
        val rows = (height + step - 1) / step
        return memScoped {
            val buffer = allocArray<UByteVar>(columns * rows * 4)
            val space = CGColorSpaceCreateDeviceRGB()
            val context =
                CGBitmapContextCreate(
                    buffer,
                    columns.convert(),
                    rows.convert(),
                    8u,
                    (columns * 4).convert(),
                    space,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
                )
            CGColorSpaceRelease(space)
            if (context == null) return@memScoped null
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, columns.toDouble(), rows.toDouble()), image)
            CGContextRelease(context)
            IntArray(columns * rows) { i ->
                val r = buffer[i * 4].toInt()
                val g = buffer[i * 4 + 1].toInt()
                val b = buffer[i * 4 + 2].toInt()
                val a = buffer[i * 4 + 3].toInt()
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private companion object {
        const val MAX_SIDE = 128
    }
}
