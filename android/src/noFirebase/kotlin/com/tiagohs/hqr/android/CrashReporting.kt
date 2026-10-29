package com.tiagohs.hqr.android

import android.app.Application

/** Without google-services.json there is no Crashlytics (ADR-014): nothing is sent. */
object CrashReporting {
    @Suppress("UnusedParameter") // Same signature as the Firebase variant.
    fun init(app: Application) = Unit
}
