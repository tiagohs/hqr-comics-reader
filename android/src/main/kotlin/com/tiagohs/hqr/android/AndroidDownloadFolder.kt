package com.tiagohs.hqr.android

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.tiagohs.hqr.data.download.DownloadExport
import com.tiagohs.hqr.data.download.FreeSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.sink
import java.io.File

/** RF-67: the copy of a finished chapter, written in the folder the user picked (Storage Access Framework). */
class SafDownloadExport(
    private val context: Context,
) : DownloadExport {
    override suspend fun save(
        folder: String,
        work: String,
        fileName: String,
        write: suspend (okio.Sink) -> Unit,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val tree = Uri.parse(folder)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val dir =
                child(tree, root, work)
                    ?: DocumentsContract.createDocument(resolver, root, Document.MIME_TYPE_DIR, work)
                    ?: return@withContext false
            // Written under a temporary name first: a copy that fails never costs the one already there.
            val partName = "$fileName.part"
            child(tree, dir, partName)?.let { DocumentsContract.deleteDocument(resolver, it) }
            val part = DocumentsContract.createDocument(resolver, dir, MIME, partName) ?: return@withContext false
            try {
                val out = resolver.openOutputStream(part) ?: error("no output")
                out.sink().use { write(it) }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                DocumentsContract.deleteDocument(resolver, part)
                throw e
            }
            // Downloaded again: the new file takes the old one's place, never "name (1).cbz".
            child(tree, dir, fileName)?.let { DocumentsContract.deleteDocument(resolver, it) }
            DocumentsContract.renameDocument(resolver, part, fileName) != null
        }

    private fun child(
        tree: Uri,
        parent: Uri,
        name: String,
    ): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
            }
        }
        return null
    }

    private companion object {
        // A type the providers keep as given, so the name stays ".cbz".
        const val MIME = "application/octet-stream"
    }
}

/** RF-68: free space where the downloads are. */
class AndroidFreeSpace(
    private val dir: File,
) : FreeSpace {
    override fun bytes(): Long? = runCatching { StatFs(dir.path).availableBytes }.getOrNull()
}
