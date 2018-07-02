package com.tiagohs.hqr.download

import android.content.Context
import com.google.gson.Gson
import com.tiagohs.hqr.database.IChapterRepository
import com.tiagohs.hqr.database.IComicsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.sources.SourceManager

class DownloadStore(
        context: Context?,
        val sourceManager: SourceManager,
        val sourceRepository: ISourceRepository,
        val comicsRepository: IComicsRepository,
        val chapterRepository: IChapterRepository,
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
        val obj = DownloadObject(download.comic.id, download.chapter.chapterPath!!, download.source.id)
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
            for (( comicId, chapterPath, sourceId) in downloadObject) {
                val sourceHttp = sourceManager.get(sourceId)
                val source = sourceRepository.getSourceByIdRealm(sourceId)
                val comic = comicsRepository.findByIdRealm(comicId)
                val chapter = chapterRepository.getChapterRealm(chapterPath, comicId)

                downloads.add(Download(sourceHttp!!, source!!, comic!!, chapter!!))
            }
        }

        clear()
        return downloads
    }

    data class DownloadObject(val comicId: Long, val chapterPath: String, var sourceId: Long)
}