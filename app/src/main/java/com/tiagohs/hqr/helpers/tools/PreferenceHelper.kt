package com.tiagohs.hqr.helpers.tools

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.tiagohs.hqr.R
import java.io.File

fun <T> Preference<T>.getOrDefault(): T = get() ?: defaultValue()!!

fun Preference<Boolean>.invert(): Boolean = getOrDefault().let { set(!it); !it }

class PreferenceHelper(
        val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val rxPrefs = RxSharedPreferences.create(prefs)

    private val defaultDownloadsDir = Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name), "downloads"))

    fun clear() = prefs.edit().clear().apply()

    fun downloadsDirectory() = rxPrefs.getString(context.getString(R.string.key_download_directory), defaultDownloadsDir.toString())
    fun downloadOnlyOverWifi() = prefs.getBoolean(context.getString(R.string.key_download_only_wifi), true)
    fun checkForNewUpdatesAutomatic() = prefs.getBoolean(context.getString(R.string.key_check_update_automatic), true)
    fun language() = prefs.getString(context.getString(R.string.key_language), "en")
    fun updateHqsInProgress() = rxPrefs.getBoolean(context.getString(R.string.key_update_hqs_in_progress), true)

    fun currentSource() = rxPrefs.getLong(context.getString(R.string.key_default_source), 1L)

    fun setCurrentSource(sourceId: Long) {
        currentSource().set(sourceId)
    }
}