package com.tiagohs.hqr.ui.di

/** RNF-10: turns the crash reports on or off; only where the app sends them (Android with Firebase). */
fun interface CrashReports {
    fun setEnabled(on: Boolean)
}
