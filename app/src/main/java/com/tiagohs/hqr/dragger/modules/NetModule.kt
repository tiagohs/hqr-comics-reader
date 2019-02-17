package com.tiagohs.hqr.dragger.modules

import android.app.Application
import com.tiagohs.hqr.helpers.tools.CallInterceptor
import com.tiagohs.hqr.updater.GithubUpdaterChecker
import com.tiagohs.hqr.updater.GithubUpdaterService
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
                .addInterceptor(CallInterceptor())
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cache(cache)
                .build()
    }

    @Provides
    internal fun providesRxJava2CallAdapterFactory(): RxJava2CallAdapterFactory {
        return RxJava2CallAdapterFactory.create()
    }

    @Provides
    internal fun provideGithubUpdaterService(client: OkHttpClient, factory: RxJava2CallAdapterFactory): GithubUpdaterService {
        return GithubUpdaterService.create(client)
    }

    @Provides
    internal fun provideGithubUpdaterChecker(service: GithubUpdaterService): GithubUpdaterChecker {
        return GithubUpdaterChecker(service)
    }
}