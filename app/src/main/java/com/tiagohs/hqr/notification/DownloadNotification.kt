package com.tiagohs.hqr.notification

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.chop
import com.tiagohs.hqr.helpers.extensions.notificationManager
import com.tiagohs.hqr.helpers.utils.NotificationUtils
import com.tiagohs.hqr.models.Download
import com.tiagohs.hqr.models.DownloadQueueList

class DownloadNotification(
        val context: Context
) {

    private val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_DOWNLOADER)
                                        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

    private var isDownloading = false

    var initialQueueSize = 0
    var isErrorThrow = false
    var paused = false

    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_DOWNLOAD_CHAPTER) {
        context.notificationManager.notify(id, build())
    }

    private fun clearActions() = with(notification) {
        if (mActions.isNotEmpty()) {
            mActions.clear()
        }
    }

    private fun isFirstCall(): Boolean {
        return !isDownloading
    }

    fun dimissNotification(id: Int = Notifications.ID_DOWNLOAD_CHAPTER) {
        context.notificationManager.cancel(id)
    }

    fun onDownloadProgressChange(download: Download) {

        with(notification) {

            if (isFirstCall()) {
                setSmallIcon(R.drawable.ic_stat_launcher)
                setAutoCancel(false)
                clearActions()
                setContentIntent(NotificationUtils.onOpenDownloadManagerPending(context))

                isDownloading = true
            }

            val title = download.comic.name!!.chop(20)
            val chapterTitle = download.chapter.chapterName
            val notificationTitle = "$title - $chapterTitle".chop(30)

            setContentTitle(notificationTitle)
            setContentText(context.getString(R.string.chapter_downloading_progress)
                    .format(download.numberOfImagesDownloaded, download.chapter.pages!!.size))
            setProgress(download.chapter.pages!!.size, download.numberOfImagesDownloaded, false)
        }

        notification.show()
    }

    fun onDownloadPaused() {

        with(notification) {
            setContentTitle(context.getString(R.string.download_paused))
            setContentText(context.getString(R.string.download_paused))
            setAutoCancel(false)
            setProgress(0,0, false)
            clearActions()
            setContentIntent(NotificationUtils.onOpenDownloadManagerPending(context))
            addAction(R.drawable.ic_play_arrow_grey,
                    context.getString(R.string.action_resume),
                    NotificationReceiver.resumeDownloadsPendingBroadcast(context))
            addAction(R.drawable.ic_clear_grey,
                    context.getString(R.string.action_clear),
                    NotificationReceiver.clearDownloadsPendingBroadcast(context))

        }

        notification.show()

        isDownloading = false
        initialQueueSize = 0
    }

    fun onDownloadCompleted(download: Download, queue: DownloadQueueList) {

        if (queue.isNotEmpty()) {
            return
        }

        with(notification) {
            val title = download.comic.name?.chop(20)
            val chapterTitle = download.chapter.chapterName

            setContentTitle("$title - $chapterTitle".chop(30))
            setContentText(context.getString(R.string.download_complete))
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationReceiver.openChapterPedingBroadcast(context, download.comic, download.chapter))
            setProgress(0, 0, false)
        }

        notification.show()

        isDownloading = false
        initialQueueSize = 0
    }

    fun onWarning(reason: String) {
        with(notification) {
            setContentTitle(context.getString(R.string.download_notification_downloader_title))
            setContentText(reason)
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            clearActions()
            setContentIntent(NotificationUtils.onOpenDownloadManagerPending(context))
            setProgress(0, 0, false)
        }
        notification.show()

        isDownloading = false
    }

    fun onError(error: String? = null, chapter: String? = null) {
        with(notification) {
            setContentTitle(chapter ?: context.getString(R.string.download_notification_downloader_title))
            setContentText(error ?: context.getString(R.string.download_notification_unkown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            clearActions()
            setAutoCancel(false)
            setContentIntent(NotificationUtils.onOpenDownloadManagerPending(context))
            setProgress(0, 0, false)
        }
        notification.show(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)

        isErrorThrow = true
        isDownloading = false
    }

}