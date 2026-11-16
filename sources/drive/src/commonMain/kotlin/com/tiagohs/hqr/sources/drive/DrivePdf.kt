package com.tiagohs.hqr.sources.drive

import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A PDF served by the Worker, turned into page images on the device (RF-131). */
interface PdfRenderer {
    suspend fun pageCount(
        url: String,
        token: String,
    ): Int

    suspend fun render(
        url: String,
        token: String,
        page: Int,
    ): ByteArray
}

/** The system renderer of one platform over the bytes of a PDF: Android PdfRenderer, Core Graphics. */
interface PdfEngine {
    suspend fun pageCount(
        key: String,
        bytes: ByteArray,
    ): Int

    suspend fun render(
        key: String,
        bytes: ByteArray,
        page: Int,
    ): ByteArray
}

/**
 * Downloads each PDF once and keeps the last one, so the pages of a chapter read in a row never fetch it again; the
 * pages come from the platform's [engine].
 */
class DownloadedPdfRenderer(
    private val http: HttpClient,
    private val engine: PdfEngine,
) : PdfRenderer {
    private val lock = Mutex()
    private var last: Pair<String, CompletableDeferred<ByteArray>>? = null

    override suspend fun pageCount(
        url: String,
        token: String,
    ): Int = engine.pageCount(url, bytes(url, token))

    override suspend fun render(
        url: String,
        token: String,
        page: Int,
    ): ByteArray = engine.render(url, bytes(url, token), page)

    private suspend fun bytes(
        url: String,
        token: String,
    ): ByteArray {
        val (pending, owner) =
            lock.withLock {
                last?.takeIf { it.first == url }?.let { it.second to false }
                    ?: CompletableDeferred<ByteArray>().also { last = url to it } to true
            }
        if (!owner) return pending.await()
        try {
            val loaded =
                networkCall {
                    http.get(url) { header(HttpHeaders.Authorization, "Bearer $token") }.ensureOk().bodyAsBytes()
                }
            pending.complete(loaded)
            return loaded
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Throwable,
        ) {
            // A failed download is not kept: the next page tries again.
            lock.withLock { if (last?.second === pending) last = null }
            pending.completeExceptionally(e)
            throw e
        }
    }
}
