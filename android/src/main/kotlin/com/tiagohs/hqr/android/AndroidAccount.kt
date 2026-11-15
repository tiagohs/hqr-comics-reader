package com.tiagohs.hqr.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tiagohs.hqr.domain.repository.AccountRepository
import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/** The activity on screen, for the Google sheet; nothing else keeps it. */
class CurrentActivity : Application.ActivityLifecycleCallbacks {
    private var current = WeakReference<Activity>(null)

    fun get(): Activity? = current.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current.get() === activity) current = WeakReference(null)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

/** RF-119 on Android: Credential Manager's "Sign in with Google" sheet, for the Worker's web client (ADR-028). */
class CredentialManagerSignIn(
    private val serverClientId: String,
    private val activity: () -> Activity?,
) : GoogleIdTokens {
    override suspend fun request(): String? {
        val screen = activity() ?: return null
        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
                .build()
        return try {
            val credential = CredentialManager.create(screen).getCredential(screen, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        } catch (
            @Suppress("SwallowedException") e: GetCredentialException,
        ) {
            // Closed by the user or no Google account on the device: nothing changes.
            null
        }
    }
}

/** RF-122: every 6 h, with network, the account syncs in the background. */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val account: AccountRepository by inject()

    override suspend fun doWork(): Result {
        if (account.account.value is AccountState.SignedIn) account.syncNow()
        return Result.success()
    }

    companion object {
        private const val NAME = "account-sync"
        private const val HOURS = 6L

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(HOURS, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
