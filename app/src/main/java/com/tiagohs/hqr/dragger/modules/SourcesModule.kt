package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.sources.SourceManager
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

@Module
class SourcesModule {

    @Provides
    fun providerSourceManager(client: OkHttpClient, chapterCache: ChapterCache): SourceManager {
        return SourceManager(client, chapterCache)
    }

}