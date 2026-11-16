package com.tiagohs.hqr.android

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tiagohs.hqr.sources.drive.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * RF-131: the system PdfRenderer needs a file, so the PDF of the Drive is written once to [dir] (the last one stays,
 * the others go) and its pages are drawn one at a time, since the renderer opens a single page at a time.
 */
class DrivePdfEngine(
    private val dir: File,
) : PdfEngine {
    private val lock = Mutex()

    override suspend fun pageCount(
        key: String,
        bytes: ByteArray,
    ): Int = open(key, bytes) { it.pageCount }

    override suspend fun render(
        key: String,
        bytes: ByteArray,
        page: Int,
    ): ByteArray =
        open(key, bytes) { renderer ->
            renderer.openPage(page).use { p ->
                val bitmap = Bitmap.createBitmap(p.width * SCALE, p.height * SCALE, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
            }
        }

    private suspend fun <T> open(
        key: String,
        bytes: ByteArray,
        block: (PdfRenderer) -> T,
    ): T =
        lock.withLock {
            withContext(Dispatchers.IO) {
                val file = fileFor(key, bytes)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd -> PdfRenderer(pfd).use(block) }
            }
        }

    private fun fileFor(
        key: String,
        bytes: ByteArray,
    ): File {
        dir.mkdirs()
        val file = File(dir, "${key.hashCode().toUInt().toString(HEX)}.pdf")
        if (!file.exists() || file.length() != bytes.size.toLong()) {
            dir.listFiles()?.forEach { it.delete() }
            file.writeBytes(bytes)
        }
        return file
    }

    private companion object {
        const val SCALE = 2
        const val JPEG_QUALITY = 90
        const val HEX = 16
    }
}
