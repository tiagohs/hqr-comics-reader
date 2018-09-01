package com.tiagohs.hqr.updater

import android.content.Intent
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.tiagohs.hqr.App
import com.tiagohs.hqr.dragger.components.HQRComponent
import com.tiagohs.hqr.helpers.tools.CallInterceptor
import com.tiagohs.hqr.notification.UpdaterNotification
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UpdaterJob: Job() {


    @Inject
    lateinit var client: OkHttpClient

    @Inject
    lateinit var notifier: UpdaterNotification

    override fun onRunJob(params: Params): Result {

        getApplicationComponent()?.inject(this)

        return GithubUpdaterChecker(GithubUpdaterService.create(client))
                .checkForUpdate()
                .map { result ->

                    if (result is GithubVersionResults.NewUpdate) {
                        val url = result.release.assets[0].downloadLink

                        val intent = Intent(context, UpdaterService::class.java).apply {
                            putExtra(UpdaterService.EXTRA_UPDATER_DOWNLOAD_URL, url)
                        }

                        notifier.newUpdateAvailable(intent)
                    }

                    Job.Result.SUCCESS
                }
                .onErrorReturn { Job.Result.FAILURE }
                .blockingFirst()

    }

    private fun getApplicationComponent(): HQRComponent? {
        return (context.applicationContext as App).getHQRComponent()
    }

    companion object {
        const val TAG = "UpdateChecker"

        fun setupTask() {
            JobRequest.Builder(TAG)
                    .setPeriodic(24 * 60 * 60 * 1000, 60 * 60 * 1000)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }

        fun cancelTask() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }
}