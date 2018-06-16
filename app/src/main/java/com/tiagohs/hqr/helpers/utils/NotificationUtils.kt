package com.tiagohs.hqr.helpers.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tiagohs.hqr.ui.views.activities.RootActivity

object NotificationUtils {

    fun onOpenDownloadManagerPending(context: Context): PendingIntent {
        val intent = Intent(context, RootActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            action = "SHORTCUT_DOWNLOADS"
        }

        return PendingIntent.getActivity(context, 0, intent, 0)
    }

}