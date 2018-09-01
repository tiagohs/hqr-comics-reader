package com.tiagohs.hqr.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

object NotificationHandler {


    fun installApkPendingActivity(context: Context, uri: Uri): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, 0)
    }
}