@file:Suppress("UnusedParameter") // The JavaScript bodies read the parameters by name.

package com.tiagohs.hqr.web

import com.tiagohs.hqr.sources.local.FileNode
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.ImportResult
import com.tiagohs.hqr.sources.local.ImportedFiles
import com.tiagohs.hqr.sources.local.PdfPages
import com.tiagohs.hqr.sources.local.StorageUsage
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.js.Promise

private fun filesAvailable(): Boolean = js("!!(window.hqrFiles && window.hqrFiles.available)")

private fun jsPick(): Promise<JsString> = js("window.hqrFiles.pick()")

private fun jsList(): Promise<JsString> = js("window.hqrFiles.list()")

private fun jsUsage(): Promise<JsString> = js("window.hqrFiles.usage()")

private fun jsPersist(): Promise<JsBoolean> = js("window.hqrFiles.persist()")

private fun jsRead(
    name: String,
    offset: Double,
    length: Int,
): Promise<Int8Array> = js("window.hqrFiles.read(name, offset, length)")

private fun jsPdfPageCount(name: String): Promise<JsNumber> = js("window.hqrFiles.pdfPageCount(name)")

private fun jsPdfRender(
    name: String,
    page: Int,
): Promise<Int8Array> = js("window.hqrFiles.pdfRender(name, page)")

private fun jsOnChange(callback: () -> Unit): Unit = js("window.hqrFiles.onChange(callback)")

private fun jsOnDrop(callback: (JsString) -> Unit): Unit = js("window.hqrFiles.onDrop(callback)")

@Serializable
private data class Entry(
    val name: String,
    val size: Long,
    val modified: Double,
)

@Serializable
private data class Imported(
    val imported: List<String> = emptyList(),
    val refused: List<String> = emptyList(),
)

@Serializable
private data class Usage(
    val used: Double,
    val quota: Double,
)

/**
 * The files imported into the browser storage, OPFS (RF-110, FEAT-052): a flat folder read by ranges, so an archive
 * of 80 MB never enters memory whole.
 */
class BrowserFileTree :
    FileTree,
    ImportedFiles,
    PdfPages {
    private val json = Json { ignoreUnknownKeys = true }
    private val available = filesAvailable()
    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()
    private val drops = Channel<ImportResult>(Channel.BUFFERED)
    override val dropped: Flow<ImportResult> = drops.receiveAsFlow()

    init {
        if (available) {
            jsOnChange { _changes.value++ }
            jsOnDrop { text -> drops.trySend(parse(text.toString())) }
        }
    }

    private fun parse(text: String): ImportResult =
        json.decodeFromString(Imported.serializer(), text).let { ImportResult(it.imported, it.refused) }

    private suspend fun entries(): List<Entry> {
        if (!available) return emptyList()
        val text = jsList().await<JsString>().toString()
        return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Entry.serializer()), text)
    }

    override suspend fun isReady(): Boolean = entries().isNotEmpty()

    override suspend fun children(path: String): List<FileNode> =
        if (path.isNotEmpty()) {
            emptyList()
        } else {
            entries().map { FileNode(it.name, it.name, isDirectory = false, size = it.size, lastModifiedEpochMs = it.modified.toLong()) }
        }

    override suspend fun size(path: String): Long = entries().firstOrNull { it.name == path }?.size ?: 0

    override suspend fun read(
        path: String,
        offset: Long,
        length: Int,
    ): ByteArray {
        val array = jsRead(path, offset.toDouble(), length).await<Int8Array>()
        return ByteArray(array.length) { array[it] }
    }

    override suspend fun pageCount(path: String): Int = jsPdfPageCount(path).await<JsNumber>().toInt()

    override suspend fun render(
        path: String,
        page: Int,
    ): ByteArray = jsPdfRender(path, page).await<Int8Array>().let { array -> ByteArray(array.length) { array[it] } }

    override suspend fun pick(): ImportResult =
        if (available) parse(jsPick().await<JsString>().toString()) else ImportResult(emptyList(), emptyList())

    override suspend fun usage(): StorageUsage? =
        if (available) {
            json
                .decodeFromString(Usage.serializer(), jsUsage().await<JsString>().toString())
                .let { StorageUsage(it.used.toLong(), it.quota.toLong()) }
        } else {
            null
        }

    override suspend fun persist(): Boolean = available && jsPersist().await<JsBoolean>().toBoolean()
}
