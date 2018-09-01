package com.tiagohs.hqr.updater

import com.tiagohs.hqr.BuildConfig
import io.reactivex.Observable

class GithubUpdaterChecker(
        val service: GithubUpdaterService
) {

    fun checkForUpdate(): Observable<GithubVersionResults> {
        return service.getLastestVersion()
                .map { release ->
                    val newVersion = release.version.replace("[^\\d.]".toRegex(), "")

                    if (newVersion != BuildConfig.VERSION_NAME) {
                        GithubVersionResults.NewUpdate(release)
                    } else {
                        GithubVersionResults.NoNewUpdate()
                    }
                }
    }
}