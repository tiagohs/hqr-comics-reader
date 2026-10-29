package com.tiagohs.hqr.android

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Crashlytics (ADR-014, RNF-07): no custom keys with content; the user can turn it off in Settings. */
object CrashReporting {
    fun init(app: Application) {
        val enabled = app.getSharedPreferences("hqr", Application.MODE_PRIVATE).getBoolean("crash_reports", true)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
    }
}
