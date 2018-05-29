package com.tiagohs.hqr.dragger.modules

import android.app.Application
import com.tiagohs.hqr.dragger.scopes.PerFragment
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

@Module
class NetModule {

    @Provides
    internal fun provideOkHttpCache(application: Application): Cache {
        val cacheSize: Long = 10 * 1024 * 1024 // 10 MiB
        return Cache(application.cacheDir, cacheSize)
    }

    @Provides
    internal fun provideOkHttpClient(cache: Cache): OkHttpClient {

        return OkHttpClient.Builder()
                .cache(cache)
                .build()
    }

    @PerFragment
    @Provides
    internal fun providesRxJava2CallAdapterFactory(): RxJava2CallAdapterFactory {
        return RxJava2CallAdapterFactory.create()
    }

}