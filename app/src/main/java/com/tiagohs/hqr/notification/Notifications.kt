package com.tiagohs.hqr.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.notificationManager

object Notifications {

    const val CHANNEL_DOWNLOADER = "downloader_channel"
    const val ID_DOWNLOAD_CHAPTER = 201
    const val ID_DOWNLOAD_CHAPTER_ERROR = 202

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
                NotificationChannel(CHANNEL_DOWNLOADER, context.getString(R.string.channel_downloader),
                        NotificationManager.IMPORTANCE_LOW)
        )
        context.notificationManager.createNotificationChannels(channels)
    }
}