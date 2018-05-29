package com.tiagohs.hqr.dragger.modules

import com.tiagohs.hqr.sources.portuguese.HQBRSource
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

@Module
class SourcesModule {

    @Provides
    fun providesHQBRSource(client: OkHttpClient): HQBRSource {
        return HQBRSource(client)
    }
}