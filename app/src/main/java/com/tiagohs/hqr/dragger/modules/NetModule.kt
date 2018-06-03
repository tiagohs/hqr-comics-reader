package com.tiagohs.hqr.dragger.modules

import android.app.Application
import com.tiagohs.hqr.dragger.scopes.PerFragment
import com.tiagohs.hqr.service.CallInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.TimeUnit

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
                .addInterceptor(CallInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cache(cache)
                .build()
    }

    @PerFragment
    @Provides
    internal fun providesRxJava2CallAdapterFactory(): RxJava2CallAdapterFactory {
        return RxJava2CallAdapterFactory.create()
    }

}