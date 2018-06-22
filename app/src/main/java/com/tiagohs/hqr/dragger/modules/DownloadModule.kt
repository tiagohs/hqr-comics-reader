package com.tiagohs.hqr.dragger.modules

import android.content.Context
import com.tiagohs.hqr.download.DownloadCache
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.download.DownloadStore
import com.tiagohs.hqr.download.Downloader
import com.tiagohs.hqr.notification.DownloadNotification
import com.tiagohs.hqr.sources.SourceManager
import dagger.Module
import dagger.Provides

@Module
class DownloadModule {

    @Provides
    fun providerDownloadProvider(context: Context): DownloadProvider {
        return DownloadProvider(context)
    }

    @Provides
    fun providerDownloadCache(context: Context, sourceManager: SourceManager, provider: DownloadProvider): DownloadCache {
        return DownloadCache(context, sourceManager, provider)
    }

    @Provides
    fun providerDownloader(context: Context, provider: DownloadProvider, sourceManager: SourceManager, store: DownloadStore, cache: DownloadCache, downloadNotification: DownloadNotification): Downloader {
        return Downloader(context, provider, sourceManager, store, cache, downloadNotification)
    }

    @Provides
    fun providerDownloadStore(context: Context, sourceManager: SourceManager): DownloadStore {
        return DownloadStore(context, sourceManager)
    }


}