package com.tiagohs.hqr.ui.color

import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.source.ImageBytes
import com.tiagohs.hqr.presentation.color.CoverPixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Reads the cover through the image cache and samples it down to at most [MAX_SIDE]² pixels (RF-98). */
class ImageCoverPixels(
    private val images: ImageBytes,
) : CoverPixels {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun pixels(manga: Manga): IntArray? {
        val url = manga.source.coverUrl ?: return null
        val bytes = images.load(manga.sourceId, url, emptyMap())
        // Decoding and sampling stay off the main thread (the work screen is opening).
        return withContext(Dispatchers.Default) {
            val bitmap = bytes.decodeToImageBitmap()
            val all = IntArray(bitmap.width * bitmap.height)
            bitmap.readPixels(all)
            val step = maxOf(1, maxOf(bitmap.width, bitmap.height) / MAX_SIDE)
            val columns = (bitmap.width + step - 1) / step
            val rows = (bitmap.height + step - 1) / step
            IntArray(columns * rows) { i -> all[(i / columns) * step * bitmap.width + (i % columns) * step] }
        }
    }

    private companion object {
        const val MAX_SIDE = 128
    }
}
