package com.tiagohs.hqr.android

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.ui.di.CrashReports

/** Crashlytics (ADR-014, RNF-07): no custom keys with content; the user can turn it off in Settings. */
object CrashReporting {
    fun init(app: Application) {
        val enabled = app.getSharedPreferences("hqr", Application.MODE_PRIVATE).getBoolean(AppPreferences.CRASH_REPORTS, true)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
    }

    /** The switch in Settings. */
    val reports: CrashReports? = CrashReports { FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = it }
}
