package com.tiagohs.hqr.ui.components

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.memory.MemoryCache
import coil3.request.Options
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.ImageBytes
import okio.Buffer

/** A cover or page to draw: Coil asks [ImageBytes], which knows cache, downloads and each source (ADR-006). */
data class ImageRef(
    val sourceId: SourceId,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

private class ImageRefFetcher(
    private val ref: ImageRef,
    private val options: Options,
    private val bytes: ImageBytes,
) : Fetcher {
    override suspend fun fetch(): FetchResult =
        SourceFetchResult(
            source = ImageSource(Buffer().write(bytes.load(ref.sourceId, ref.url, ref.headers)), options.fileSystem),
            mimeType = null,
            dataSource = DataSource.NETWORK,
        )

    class Factory(
        private val bytes: ImageBytes,
    ) : Fetcher.Factory<ImageRef> {
        override fun create(
            data: ImageRef,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ImageRefFetcher(data, options, bytes)
    }
}

/** The one image loader of the app: memory cache only, the disk cache lives in [ImageBytes]. */
fun hqrImageLoader(
    context: PlatformContext,
    bytes: ImageBytes,
): ImageLoader =
    ImageLoader
        .Builder(context)
        .components { add(ImageRefFetcher.Factory(bytes)) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
        .build()
