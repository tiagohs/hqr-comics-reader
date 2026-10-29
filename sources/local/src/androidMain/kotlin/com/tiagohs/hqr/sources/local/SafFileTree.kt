package com.tiagohs.hqr.sources.local

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Cursor columns, in the order of the projection below.
private const val COL_MIME = 2
private const val COL_SIZE = 3
private const val COL_MODIFIED = 4
private const val JPEG_QUALITY = 90

/**
 * The chosen folder through the Storage Access Framework (RF-13), read only (RF-16). Paths are relative to the
 * tree root, so the work key survives when the same folder is chosen again.
 */
class SafFileTree(
    private val resolver: ContentResolver,
    private val treeUri: () -> String?,
) : FileTree {
    // Path → document id, per chosen folder: Coil reads covers in parallel, and the ids of one folder never serve
    // another one.
    private val caches = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>()
    private val cache: MutableMap<String, String>
        get() = caches.getOrPut(treeUri().orEmpty()) { java.util.concurrent.ConcurrentHashMap() }

    override suspend fun isReady(): Boolean =
        withContext(Dispatchers.IO) {
            val tree = treeUri() ?: return@withContext false
            runCatching { children(rootDocument(tree), "") }.isSuccess
        }

    private fun rootDocument(tree: String): String = DocumentsContract.getTreeDocumentId(Uri.parse(tree))

    private fun documentId(path: String): String {
        val tree = requireNotNull(treeUri()) { "Nenhuma pasta escolhida" }
        if (path.isEmpty()) return rootDocument(tree)
        return cache[path] ?: error("Caminho desconhecido: $path")
    }

    override suspend fun children(path: String): List<FileNode> =
        withContext(Dispatchers.IO) {
            treeUri() ?: return@withContext emptyList()
            if (path.isNotEmpty() && path !in cache) resolve(path)
            children(documentId(path), path)
        }

    private fun children(
        parentDocument: String,
        parentPath: String,
    ): List<FileNode> {
        val tree = Uri.parse(requireNotNull(treeUri()))
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocument)
        val projection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
        val out = ArrayList<FileNode>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                val path = if (parentPath.isEmpty()) name else "$parentPath/$name"
                cache[path] = id
                out +=
                    FileNode(
                        path = path,
                        name = name,
                        isDirectory = c.getString(COL_MIME) == DocumentsContract.Document.MIME_TYPE_DIR,
                        size = if (c.isNull(COL_SIZE)) 0 else c.getLong(COL_SIZE),
                        lastModifiedEpochMs = if (c.isNull(COL_MODIFIED)) 0 else c.getLong(COL_MODIFIED),
                    )
            }
        }
        return out
    }

    /** Walks from the root to [path] filling the id cache (after a restart the cache is empty). */
    private fun resolve(path: String) {
        var parent = ""
        for (part in path.split('/')) {
            val current = if (parent.isEmpty()) part else "$parent/$part"
            if (current !in cache) children(documentId(parent), parent)
            parent = current
        }
    }

    internal fun idOf(path: String): String {
        if (path !in cache) resolve(path)
        return documentId(path)
    }

    private fun fileUri(path: String): Uri {
        if (path !in cache) resolve(path)
        return DocumentsContract.buildDocumentUriUsingTree(Uri.parse(requireNotNull(treeUri())), documentId(path))
    }

    override suspend fun size(path: String): Long =
        withContext(Dispatchers.IO) {
            resolver.openFileDescriptor(fileUri(path), "r")?.use { it.statSize } ?: 0
        }

    override suspend fun read(
        path: String,
        offset: Long,
        length: Int,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            resolver.openFileDescriptor(fileUri(path), "r")!!.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                    val buffer = java.nio.ByteBuffer.allocate(length)
                    var position = offset
                    while (buffer.hasRemaining()) {
                        val n = channel.read(buffer, position)
                        if (n <= 0) break
                        position += n
                    }
                    buffer.array().copyOf(buffer.position())
                }
            }
        }
}

/** PDF pages through the system renderer (RF-13, ADR-011). */
class AndroidPdfPages(
    private val resolver: ContentResolver,
    private val tree: SafFileTree,
    private val treeUri: () -> String?,
) : PdfPages {
    override suspend fun pageCount(path: String): Int =
        withContext(Dispatchers.IO) {
            open(path) { it.pageCount }
        }

    override suspend fun render(
        path: String,
        page: Int,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            open(path) { renderer ->
                renderer.openPage(page).use { p ->
                    val scale = 2
                    val bitmap =
                        android.graphics.Bitmap.createBitmap(
                            p.width * scale,
                            p.height * scale,
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    p.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    java.io
                        .ByteArrayOutputStream()
                        .also {
                            bitmap.compress(
                                android.graphics.Bitmap.CompressFormat.JPEG,
                                JPEG_QUALITY,
                                it,
                            )
                        }.toByteArray()
                }
            }
        }

    private suspend fun <T> open(
        path: String,
        block: (android.graphics.pdf.PdfRenderer) -> T,
    ): T {
        tree.children(path.substringBeforeLast('/', ""))
        val doc = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(requireNotNull(treeUri())), tree.idOf(path))
        return resolver.openFileDescriptor(doc, "r")!!.use { pfd ->
            android.graphics.pdf
                .PdfRenderer(pfd)
                .use(block)
        }
    }
}
