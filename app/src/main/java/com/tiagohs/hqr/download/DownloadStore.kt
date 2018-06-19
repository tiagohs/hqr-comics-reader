package com.tiagohs.hqr.download

import android.content.Context
import com.google.gson.Gson
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.sources.HttpSourceBase
import com.tiagohs.hqr.sources.SourceManager

class DownloadStore(
        context: Context?,
        val sourceManager: SourceManager
) {
    private val preferences = context?.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)
    private val gson: Gson = Gson()

    fun addAll(downloads: List<Download>) {
        val editor = preferences?.edit()
        downloads.forEach { download: Download -> editor?.putString(getKey(download), serialize(download)) }
        editor?.apply()
    }

    fun remove(download: Download) {
        preferences?.edit()?.remove(getKey(download))?.apply()
    }

    fun clear() {
        preferences?.edit()?.clear()?.apply()
    }

    private fun getKey(download: Download): String {
        return download.chapter.id!!
    }

    private fun serialize(download: Download): String {
        val obj = DownloadObject(download.comic, download.chapter, download.source.id)
        return gson.toJson(obj)
    }

    private fun deserialize(string: String): DownloadObject {
        return gson.fromJson(string, DownloadObject::class.java)
    }

    fun restore(): List<Download> {
        val downloadObject = preferences?.all!!
                .mapNotNull { it.value as? String }
                .map { deserialize(it) }

        val downloads = mutableListOf<Download>()

        if (downloadObject.isNotEmpty()) {
            for (( comic, chapter, sourceId) in downloadObject) {
                val source = sourceManager.get(sourceId) as HttpSourceBase

                downloads.add(Download(source, comic, chapter))
            }
        }

        clear()
        return downloads
    }

    data class DownloadObject(val comic: Comic, val chapter: Chapter, var sourceId: Long)
}