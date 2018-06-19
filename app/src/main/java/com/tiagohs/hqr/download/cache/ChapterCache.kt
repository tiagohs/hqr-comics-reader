package com.tiagohs.hqr.download.cache

import android.content.Context
import android.text.format.Formatter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.jakewharton.disklrucache.DiskLruCache
import com.tiagohs.hqr.helpers.extensions.saveTo
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Page
import io.reactivex.Observable
import okhttp3.Response
import okio.Okio
import java.io.File
import java.io.IOException

class ChapterCache(
        private val context: Context
) {

    companion object {
        const val PARAMETER_CACHE_DIRECTORY = "disk_cache_chapter"

        const val PARAMETER_APP_VERSION = 1
        const val PARAMETER_VALUE_COUNT = 1
        const val PARAMETER_MAX_CACHE_SIZE = 75L * 1024 * 1024
    }

    private val gson: Gson = Gson()

    private val diskCache = DiskLruCache.open(File(context.cacheDir, PARAMETER_CACHE_DIRECTORY),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            PARAMETER_MAX_CACHE_SIZE)

    private val realSize: Long
        get() = DiskUtils.getDirectorySize(cacheDir)

    val cacheDir: File
        get() = diskCache.directory

    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache).
        if (file == "journal" || file.startsWith("journal."))
            return false

        try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache.
            return diskCache.remove(key)
        } catch (e: Exception) {
            return false
        }
    }

    fun getPageListFromCache(chapter: Chapter): Observable<List<Page>> {
        return Observable.fromCallable {
            onGetPageListFromGson(DiskUtils.hashKeyForDisk(getKey(chapter)))
        }
    }

    private fun onGetPageListFromGson(chapterKey: String): List<Page> {
        return diskCache.get(chapterKey).use {
            gson.fromJson(it.getString(0))
        }
    }

    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        val pageListJson = gson.toJson(pages)
        var editor: DiskLruCache.Editor? = null

        try {
            val md5EditorKey = DiskUtils.hashKeyForDisk(getKey(chapter))
            editor = diskCache.edit(md5EditorKey) ?: return

            Okio.buffer(Okio.sink(editor.newOutputStream(0))).use {
                it.write(pageListJson.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()

        } catch (e: Exception) {

        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    fun isImageInCache(imageUrl: String): Boolean {
        try {
            return diskCache.get(DiskUtils.hashKeyForDisk(imageUrl)) != null
        } catch (e: IOException) {
            return false
        }
    }

    fun getImageFile(imageUrl: String): File {
        val imageName = DiskUtils.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, response: Response) {
        var editor: DiskLruCache.Editor? = null

        try {
            val md5EditorKey = DiskUtils.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(md5EditorKey) ?: throw IOException("Unable to edit key")

            response.body()!!.source().saveTo(editor.newOutputStream(0))

            diskCache.flush()
            editor.commit()
        } finally {
            response.body()?.close()
            editor?.abortUnlessCommitted()
        }
    }

    private fun getKey(chapter: Chapter): String {
        return "${chapter.comicId}${chapter.chapterPath}"
    }
}