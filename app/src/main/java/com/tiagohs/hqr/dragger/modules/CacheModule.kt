package com.tiagohs.hqr.dragger.modules

import android.content.Context
import com.tiagohs.hqr.download.DownloadCache
import com.tiagohs.hqr.download.DownloadProvider
import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.download.cache.CoverCache
import com.tiagohs.hqr.sources.SourceManager
import dagger.Module
import dagger.Provides

@Module
class CacheModule {

    @Provides
    fun providerChapterCache(context: Context): ChapterCache {
        return ChapterCache(context)
    }

    @Provides
    fun providerCoverCache(context: Context): CoverCache {
        return CoverCache(context)
    }

}