@file:Suppress("UnusedParameter") // The JavaScript bodies read the parameters by name.

package com.tiagohs.hqr.web

import com.tiagohs.hqr.sources.drive.PdfRenderer
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.js.Promise

private fun jsPdfPageCountUrl(
    url: String,
    token: String,
): Promise<JsNumber> = js("window.hqrFiles.pdfPageCountUrl(url, token)")

private fun jsPdfRenderUrl(
    url: String,
    token: String,
    page: Int,
): Promise<Int8Array> = js("window.hqrFiles.pdfRenderUrl(url, token, page)")

/** RF-131: the PDFs of the Drive in the browser: pdf.js loads the file from the Worker and draws each page. */
class BrowserPdfRenderer : PdfRenderer {
    override suspend fun pageCount(
        url: String,
        token: String,
    ): Int = jsPdfPageCountUrl(url, token).await<JsNumber>().toInt()

    override suspend fun render(
        url: String,
        token: String,
        page: Int,
    ): ByteArray = jsPdfRenderUrl(url, token, page).await<Int8Array>().let { array -> ByteArray(array.length) { array[it] } }
}
