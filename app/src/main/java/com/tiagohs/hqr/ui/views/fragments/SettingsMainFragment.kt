package com.tiagohs.hqr.ui.views.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v4.content.ContextCompat
import android.text.format.Formatter
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.App
import com.tiagohs.hqr.R
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.helpers.extensions.getFilePicker
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.utils.DiskUtils
import java.io.File
import javax.inject.Inject

class SettingsMainFragment: PreferenceFragment() {

    companion object {
        const val DOWNLOAD_DIR_PRE_L = 103
        const val DOWNLOAD_DIR_L = 104

        fun newFragment(): SettingsMainFragment {
            return SettingsMainFragment()
        }

    }

    @Inject
    lateinit var preferences: PreferenceHelper

    @Inject
    lateinit var chapterCache: ChapterCache

    @Inject
    lateinit var downloadProvider: DownloadProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pref_main)

        getApplicationComponent()?.inject(this)

        configureListOfDirectories(findPreference(getString(R.string.key_download_directory)) as ListPreference)
        configureStorageUsed(findPreference(getString(R.string.key_download_storage_used)) as Preference)
        configureCacheStorageUsed(findPreference(getString(R.string.key_download_cache_used)) as Preference)

    }

    private fun configureListOfDirectories(preference: ListPreference) {
        val externalDirs: List<String> = getExternalDirs() + activity.getString(R.string.custom_dir)

        preference.setEntries(externalDirs.toTypedArray())
        preference.setEntryValues(externalDirs.toTypedArray())
        preference.setDefaultValue(0)

        preference.setOnPreferenceChangeListener { p, value ->
            val stringValue = value.toString()
            val index = preference.findIndexOfValue(stringValue)
            val directory = externalDirs.get(index)

            if (index == externalDirs.lastIndex) {
                customDirectorySelected(directory)
            } else {
                p.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)
                preferences.downloadsDirectory().set(directory)
            }

            true

        }
    }

    private fun configureStorageUsed(preference: Preference) {
        preference.setSummary(getString(R.string.storage_used, downloadProvider.getDownloadDirectorySize()))
        preference.setOnPreferenceClickListener { p ->
            downloadProvider.deleteAll()
            preference.setSummary(getString(R.string.storage_used, downloadProvider.getDownloadDirectorySize()))

            true
        }
    }


    private fun configureCacheStorageUsed(preference: Preference) {
        preference.setSummary(getString(R.string.storage_used, getPicassoCacheDirSize()))
        preference.setOnPreferenceClickListener { p ->
            val bol = DiskUtils.getPicassoCacheDir(activity.applicationContext)?.deleteRecursively()
            preference.setSummary(getString(R.string.storage_used, getPicassoCacheDirSize()))

            true
        }
    }

    private fun getPicassoCacheDirSize(): String {
        val picassoDir = DiskUtils.getPicassoCacheDir(activity.applicationContext)
        val size = if (picassoDir != null)
            DiskUtils.getDirectorySize(picassoDir)
        else
            0L

        return Formatter.formatFileSize(activity.applicationContext, size)
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DOWNLOAD_DIR_PRE_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                val uri = Uri.fromFile(File(data.data.path))
                preferences.downloadsDirectory().set(uri.toString())
            }
            DOWNLOAD_DIR_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                @Suppress("NewApi")
                context.contentResolver.takePersistableUriPermission(uri, flags)

                val file = UniFile.fromUri(context, uri)
                preferences.downloadsDirectory().set(file.uri.toString())
            }
        }
    }

    private fun customDirectorySelected(currentDir: String) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(preferences.context.getFilePicker(currentDir), DOWNLOAD_DIR_PRE_L)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            try {
                startActivityForResult(intent, DOWNLOAD_DIR_L)
            } catch (e: ActivityNotFoundException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivityForResult(preferences.context.getFilePicker(currentDir), DOWNLOAD_DIR_L)
                }
            }

        }
    }

    private fun getExternalDirs(): List<String> {
        val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + resources?.getString(R.string.app_name) +
                File.separator + "downloads"

        return mutableListOf(defaultDir, ContextCompat.getExternalFilesDirs(activity!!, "").filterNotNull().toString())
    }

    private fun getApplicationComponent(): HQRComponent? {
        return (activity!!.application as App).getHQRComponent()
    }
}