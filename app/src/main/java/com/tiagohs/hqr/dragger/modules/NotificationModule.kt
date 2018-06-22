package com.tiagohs.hqr.dragger.modules

import android.content.Context
import com.tiagohs.hqr.notification.DownloadNotification
import dagger.Module
import dagger.Provides

@Module
class NotificationModule {

    @Provides
    fun providerDownloadNotification(context: Context): DownloadNotification {
        return DownloadNotification(context)
    }
}