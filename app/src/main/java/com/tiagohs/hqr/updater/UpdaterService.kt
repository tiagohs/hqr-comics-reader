package com.tiagohs.hqr.updater

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tiagohs.hqr.App
import com.tiagohs.hqr.BuildConfig
import com.tiagohs.hqr.R
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.helpers.extensions.getUriCompat
import com.tiagohs.hqr.helpers.extensions.newCallWithProgress
import com.tiagohs.hqr.helpers.extensions.saveTo
import com.tiagohs.hqr.helpers.tools.GET
import com.tiagohs.hqr.helpers.tools.ProgressListener
import com.tiagohs.hqr.notification.UpdaterNotification
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class UpdaterService: IntentService(UpdaterService::class.java.name),
    ProgressListener {

    @Inject
    lateinit var notifier: UpdaterNotification

    @Inject
    lateinit var client: OkHttpClient

    private var savedProgress = 0
    private var lastTick = 0L

    override fun onCreate() {
        super.onCreate()

        getApplicationComponent()?.inject(this)
    }

    protected fun getApplicationComponent(): HQRComponent? {
        return (application as App).getHQRComponent()
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent === null) return

        val title = intent.getStringExtra(EXTRA_UPDATER_DOWNLOAD_TITLE) ?: getString(R.string.app_name)
        val url = intent.getStringExtra(EXTRA_UPDATER_DOWNLOAD_URL) ?: return

        startApkDownload(title, url)
    }

    private fun startApkDownload(title: String, url: String) {

        notifier.onDownloadStarted(title)

        try {
            val response = client.newCallWithProgress(GET(url), this).execute()

            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body()!!.source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            notifier.onDownloadFinished(apkFile.getUriCompat(this))
        } catch (error: Exception) {
            Timber.e(error)
            notifier.onDownloadError(url)
        }

    }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        val progress = (100 * bytesRead / contentLength).toInt()
        val currentTime = System.currentTimeMillis()

        if (progress > savedProgress && currentTime - 200 > lastTick) {
            savedProgress = progress
            lastTick = currentTime
            notifier.onProgressChanged(progress)
        }
    }


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

}