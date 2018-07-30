package com.tiagohs.hqr.updater

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tiagohs.hqr.BuildConfig
import com.tiagohs.hqr.R

class UpdaterService: IntentService(UpdaterService::class.java.name) {

    companion object {

        internal const val EXTRA_UPDATER_DOWNLOAD_URL = "${BuildConfig.APPLICATION_ID}.UpdaterService.UPDATER_DOWNLOAD_URL"
        internal const val EXTRA_UPDATER_DOWNLOAD_TITLE = "${BuildConfig.APPLICATION_ID}.UpdaterService.UPDATER_DOWNLOAD_TITLE"

        fun downloadUpdate(context: Context, url: String, title: String = context.getString(R.string.app_name)) {
            val intent = Intent(context, UpdaterService::class.java).apply {
                putExtra(EXTRA_UPDATER_DOWNLOAD_TITLE, title)
                putExtra(EXTRA_UPDATER_DOWNLOAD_URL, url)
            }

            context.startService(intent)
        }

        fun downloadApkPedingService(context: Context, url: String): PendingIntent {
            val intent = Intent(context, UpdaterService::class.java).apply {
                putExtra(EXTRA_UPDATER_DOWNLOAD_URL, url)
            }

            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    }

    override fun onHandleIntent(p0: Intent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}