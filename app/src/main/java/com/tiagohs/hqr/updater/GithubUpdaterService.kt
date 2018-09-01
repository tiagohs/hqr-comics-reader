package com.tiagohs.hqr.updater

import com.tiagohs.hqr.models.GithubRelease
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface GithubUpdaterService {

    companion object {
        const val BASE_GITHUB_API_URL = "https://api.github.com";

        fun create(client: OkHttpClient): GithubUpdaterService {
            val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_GITHUB_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(client)
                    .build()

            return retrofit.create(GithubUpdaterService::class.java)
        }
    }

    @GET("/repos/tiagohs/hqr-comics-reader/releases/latest")
    fun getLastestVersion(): Observable<GithubRelease>

}