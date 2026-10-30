package com.tiagohs.hqr.ui.color

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.presentation.color.CoverPixels
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Reads the cover through the image cache and samples it down to at most [MAX_SIDE]² pixels (RF-98). */
class ImageCoverPixels(
    private val images: ImageBytes,
) : CoverPixels {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun pixels(manga: Manga): IntArray? {
        val url = manga.source.coverUrl ?: return null
        val bitmap = images.load(manga.sourceId, url, emptyMap()).decodeToImageBitmap()
        val all = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(all)
        val step = maxOf(1, maxOf(bitmap.width, bitmap.height) / MAX_SIDE)
        val out = ArrayList<Int>()
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) out += all[y * bitmap.width + x]
        }
        return out.toIntArray()
    }

    private companion object {
        const val MAX_SIDE = 128
    }
}
