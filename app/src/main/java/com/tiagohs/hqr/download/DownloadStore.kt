package com.tiagohs.hqr.download

import android.content.Context
import com.google.gson.Gson
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.sources.SourceManager

class DownloadStore(
        context: Context?,
        val sourceManager: SourceManager,
        val sourceRepository: ISourceRepository,
        val preferenceHelper: PreferenceHelper
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
        return download.chapter.id.toString()
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
                val sourceId = preferenceHelper.currentSource().getOrDefault()
                val sourceHttp = sourceManager.get(sourceId)
                val source = sourceRepository.getSourceByIdRealm(sourceId)

                downloads.add(Download(sourceHttp!!, source!!, comic, chapter))
            }
        }

        clear()
        return downloads
    }

    data class DownloadObject(val comic: ComicViewModel, val chapter: ChapterViewModel, var sourceId: Long)
}