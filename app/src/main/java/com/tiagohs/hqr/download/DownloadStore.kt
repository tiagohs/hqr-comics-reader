package com.tiagohs.hqr.download

import android.content.Context
import com.google.gson.Gson
import com.tiagohs.hqr.models.Download

class DownloadStore(
        context: Context?
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
        return gson.toJson(download)
    }

}