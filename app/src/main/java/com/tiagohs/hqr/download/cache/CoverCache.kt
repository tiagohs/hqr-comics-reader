package com.tiagohs.hqr.download.cache

import android.content.Context
import com.tiagohs.hqr.helpers.utils.DiskUtils
import java.io.File
import java.io.IOException
import java.io.InputStream

class CoverCache(
        val context: Context
) {

    private val cacheDir = context.getExternalFilesDir("covers") ?:
                                File(context.filesDir, "covers").also { it.mkdirs() }

    fun getCoverFile(thumbnailUrl: String): File {
        return File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl))
    }

    @Throws(IOException::class)
    fun copyToCache(thumbnailUrl: String, inputStream: InputStream) {
        val destFile = getCoverFile(thumbnailUrl)

        destFile.outputStream().use { inputStream.copyTo(it) }
    }

    fun deleteFromCache(thumbnailUrl: String?): Boolean {
        if (thumbnailUrl.isNullOrEmpty()) return false

        val file = getCoverFile(thumbnailUrl!!)
        return file.exists() && file.delete()
    }
}