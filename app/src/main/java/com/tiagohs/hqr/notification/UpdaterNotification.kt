package com.tiagohs.hqr.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.app.NotificationCompat
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.notificationManager
import com.tiagohs.hqr.updater.UpdaterService

class UpdaterNotification(
        private val context: Context
) {
    private val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_UPDATER)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

    private fun NotificationCompat.Builder.show(id: Int = Notifications.ID_UPDATER) {
        context.notificationManager.notify(id, build())
    }

    fun onDownloadStarted(title: String) {

        with(notification) {
            setContentTitle(title)
            setContentText(context.getString(R.string.updater_notification_download_started))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }

        notification.show()
    }

    fun onDownloadFinished(uri: Uri) {

        with(notification) {
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setContentText(context.getString(R.string.updater_notification_download_finished))
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            setContentIntent(NotificationHandler.installApkPendingActivity(context, uri))
            // Install action
            addAction(R.drawable.ic_system_update_24dp,
                    context.getString(R.string.action_install),
                    NotificationHandler.installApkPendingActivity(context, uri))
            // Cancel action
            addAction(R.drawable.ic_clear_grey,
                    context.getString(R.string.action_cancel),
                    NotificationReceiver.dismissNotificationPendingBroadcast(context, Notifications.ID_UPDATER))
        }

        notification.show()
    }

    fun onDownloadError(error: String) {

        with(notification) {
            setContentText(context.getString(R.string.updater_notification_download_error))
            setSmallIcon(android.R.drawable.stat_notify_error)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            // Retry action
            addAction(R.drawable.ic_refresh_grey_24dp,
                    context.getString(R.string.action_retry),
                    UpdaterService.downloadApkPedingService(context, error))
            // Cancel action
            addAction(R.drawable.ic_clear_grey,
                    context.getString(R.string.action_cancel),
                    NotificationReceiver.dismissNotificationPendingBroadcast(context, Notifications.ID_UPDATER))
        }

        notification.show()
    }

    fun onProgressChanged(progress: Int) {

        with(notification) {
            setProgress(100, progress, false)
            setOnlyAlertOnce(true)
        }

        notification.show()
    }

    fun newUpdateAvailable(intent: Intent) {

        NotificationCompat.Builder(context, Notifications.CHANNEL_COMMON).update {
            setContentTitle(context.getString(R.string.app_name))
            setContentText(context.getString(R.string.updater_notification_download_available))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            // Download action
            addAction(android.R.drawable.stat_sys_download_done,
                    context.getString(R.string.action_download),
                    PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
        }
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        context.notificationManager.notify(Notifications.ID_UPDATER, build())
    }
}