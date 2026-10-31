@file:OptIn(ExperimentalForeignApi::class)

package com.tiagohs.hqr.app

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.download.DownloadManager
import com.tiagohs.hqr.data.download.FreeSpace
import com.tiagohs.hqr.data.net.ChallengeCookies
import com.tiagohs.hqr.domain.repository.Connectivity
import com.tiagohs.hqr.domain.repository.DownloadScheduler
import com.tiagohs.hqr.sources.local.PdfPages
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithFilename
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFDocumentRef
import platform.CoreGraphics.CGPDFDocumentRelease
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGPDFCropBox
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIRectFill
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** RF-87 and RF-64: the network as the system sees it (Network framework); "expensive" is cellular or a hotspot. */
class IosConnectivity : Connectivity {
    private val state = MutableStateFlow(true)
    private val unmetered = MutableStateFlow(true)
    override val online: StateFlow<Boolean> = state

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            state.value = nw_path_get_status(path) == nw_path_status_satisfied
            unmetered.value = !nw_path_is_expensive(path) && !nw_path_is_constrained(path)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }

    /** Downloads may run now: online, and on Wi-Fi when the user asked for it (RF-64). */
    fun allowed(wifiOnly: Boolean): Boolean = state.value && (!wifiOnly || unmetered.value)

    suspend fun awaitAllowed(wifiOnly: () -> Boolean) {
        state.first { allowed(wifiOnly()) }
    }
}

/**
 * The download queue on iOS (ADR-009): runs in the app's process while it is open or finishing in the background (the
 * Swift side asks the system for that time); a run left waiting for the network starts again when it comes back.
 */
class IosDownloadScheduler(
    private val manager: () -> DownloadManager,
    private val connectivity: IosConnectivity,
    private val wifiOnly: () -> Boolean,
) : DownloadScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null
    private var waiting: Job? = null

    override fun schedule(restart: Boolean) {
        if (restart) waiting?.cancel()
        if (running?.isActive == true || waiting?.isActive == true) return
        running =
            scope.launch {
                val left = manager().run { connectivity.allowed(wifiOnly()) }
                if (left) {
                    waiting =
                        scope.launch {
                            connectivity.awaitAllowed(wifiOnly)
                            schedule(restart = false)
                        }
                }
            }
    }

    /** True while chapters are being fetched (the Swift side keeps the app alive in the background meanwhile). */
    val isRunning: Boolean get() = running?.isActive == true
}

/** RF-68: free bytes of the volume that holds the app's data. */
class IosFreeSpace(
    private val path: String,
) : FreeSpace {
    override fun bytes(): Long? =
        (NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)?.get(NSFileSystemFreeSize) as? NSNumber)
            ?.longLongValue
}

/** RF-14: PDF pages drawn with Core Graphics, at most [WIDTH] pixels wide, as JPEG. */
class IosPdfPages(
    private val root: String,
) : PdfPages {
    override suspend fun pageCount(path: String): Int = withDocument(path) { CGPDFDocumentGetNumberOfPages(it).toInt() }

    override suspend fun render(
        path: String,
        page: Int,
    ): ByteArray =
        withDocument(path) { document ->
            val pdfPage = CGPDFDocumentGetPage(document, (page + 1).convert()) ?: error("Página $page ausente")
            val (width, height) = CGPDFPageGetBoxRect(pdfPage, kCGPDFCropBox).useContents { size.width to size.height }
            val scale = WIDTH / width
            val format = UIGraphicsImageRendererFormat().apply { setScale(1.0) }
            val renderer = UIGraphicsImageRenderer(CGSizeMake(WIDTH, height * scale), format)
            val image =
                renderer.imageWithActions { context ->
                    UIColor.whiteColor.setFill()
                    UIRectFill(CGRectMake(0.0, 0.0, WIDTH, height * scale))
                    val cg = context?.CGContext
                    CGContextTranslateCTM(cg, 0.0, height * scale)
                    CGContextScaleCTM(cg, scale, -scale)
                    CGContextDrawPDFPage(cg, pdfPage)
                }
            val data = UIImageJPEGRepresentation(image, QUALITY) ?: error("Página $page sem imagem")
            ByteArray(data.length.toInt()).also { bytes ->
                if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            }
        }

    private suspend fun <T> withDocument(
        path: String,
        block: (CGPDFDocumentRef) -> T,
    ): T =
        withContext(Dispatchers.IO) {
            val provider = CGDataProviderCreateWithFilename("$root/$path") ?: error("PDF ausente")
            val document = CGPDFDocumentCreateWithProvider(provider)
            CGDataProviderRelease(provider)
            if (document == null) error("PDF ilegível")
            try {
                block(document)
            } finally {
                CGPDFDocumentRelease(document)
            }
        }

    private companion object {
        const val WIDTH = 1600.0
        const val QUALITY = 0.9
    }
}

/**
 * RF-106: the cookies the verification page left (WKWebView keeps its own store, so the Swift side hands them over
 * here after the user passes it) and the browser's User-Agent. Saved, so a clearance outlives a restart.
 */
class IosChallengeCookies(
    private val settings: Settings,
) : ChallengeCookies {
    override val userAgent: String get() = settings.getString(USER_AGENT, "")

    override fun cookies(url: String): String? {
        val host = runCatching { Url(url).host }.getOrNull() ?: return null
        return settings.getStringOrNull(PREFIX + host)?.takeIf { ChallengeCookies.CLEARANCE in it }
    }

    /** [header] is "name=value; name2=value2" for [host]; empty clears it. */
    fun save(
        host: String,
        header: String,
        browserUserAgent: String,
    ) {
        if (header.isBlank()) settings.remove(PREFIX + host) else settings.putString(PREFIX + host, header)
        if (browserUserAgent.isNotBlank()) settings.putString(USER_AGENT, browserUserAgent)
    }

    private companion object {
        const val PREFIX = "challenge.cookies."
        const val USER_AGENT = "challenge.userAgent"
    }
}
