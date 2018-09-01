package com.tiagohs.hqr.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tiagohs.hqr.App
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.download.DownloaderService
import com.tiagohs.hqr.helpers.extensions.notificationManager
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import javax.inject.Inject

class NotificationReceiver: BroadcastReceiver() {

    companion object {
        private const val NAME = "HQRNotificationReceiver"
        private const val ID = "com.tiagohs.hqr"

        private const val ACTION_OPEN_CHAPTER = "$ID.$NAME.ACTION_OPEN_CHAPTER"
        private const val ACTION_RESUME_DOWNLOADS = "$ID.$NAME.ACTION_RESUME_DOWNLOADS"
        private const val ACTION_CLEAR_DOWNLOADS = "$ID.$NAME.ACTION_CLEAR_DOWNLOADS"
        private const val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.ACTION_DISMISS_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"
        private const val EXTRA_COMIC = "$ID.$NAME.EXTRA_COMIC"
        private const val EXTRA_CHAPTER = "$ID.$NAME.EXTRA_CHAPTER"

        fun resumeDownloadsPendingBroadcast(context: Context): PendingIntent {
            return createNotificationPendingBroadcast(context, ACTION_RESUME_DOWNLOADS)
        }

        fun clearDownloadsPendingBroadcast(context: Context): PendingIntent {
            return createNotificationPendingBroadcast(context, ACTION_CLEAR_DOWNLOADS)
        }

        fun openChapterPedingBroadcast(context: Context, comic: ComicViewModel, chapter: ChapterViewModel): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_OPEN_CHAPTER

                putExtra(EXTRA_COMIC, comic)
                putExtra(EXTRA_CHAPTER, chapter)
            }

            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun dismissNotificationPendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun createNotificationPendingBroadcast(context: Context, notificationAction: String): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = notificationAction
            }

            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    @Inject
    lateinit var downloadManager: DownloadManager

    override fun onReceive(context: Context, intent: Intent?) {
        (context.getApplicationContext() as App).getHQRComponent()?.inject(this)

        when (intent?.action) {
            ACTION_OPEN_CHAPTER -> openChapter(context, intent.getParcelableExtra(EXTRA_COMIC), intent.getParcelableExtra(EXTRA_CHAPTER))
            ACTION_RESUME_DOWNLOADS -> DownloaderService.startDownloaderService(context)
            ACTION_CLEAR_DOWNLOADS -> downloadManager.clearQueue(true)
            ACTION_DISMISS_NOTIFICATION -> dimiss(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        }
    }

    private fun dimiss(context: Context, notificationId: Int) {
        context.notificationManager.cancel(notificationId)
    }

    private fun openChapter(context: Context, comic: ComicViewModel, chapter: ChapterViewModel) {
        context.startActivity(ReaderActivity.newIntent(context, chapter.chapterPath!!, comic.pathLink!!, comic.source?.id!!))
    }
}