package com.tiagohs.hqr.helpers.extensions

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.ConnectivityManager
import android.os.PowerManager
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast

fun Context.toast(@StringRes resource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resource, duration).show()
}

fun Context.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text.orEmpty(), duration).show()
}

inline fun Context.notification(channelId: String, func: NotificationCompat.Builder.() -> Unit): Notification {
    val builder = NotificationCompat.Builder(this, channelId)
    builder.func()
    return builder.build()
}

fun Context.hasPermission(permission: String)
        = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.getResourceColor(@StringRes resource: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(resource))
    val attrValue = typedArray.getColor(0, 0)
    typedArray.recycle()
    return attrValue
}

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.connectivityManager: ConnectivityManager
    get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager

fun Context.sendLocalBroadcast(intent: Intent) {
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

fun Context.sendLocalBroadcastSync(intent: Intent) {
    LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent)
}

fun Context.registerLocalReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
}

fun Context.unregisterLocalReceiver(receiver: BroadcastReceiver) {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
}

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val className = serviceClass.name
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Integer.MAX_VALUE)
            .any { className == it.service.className }
}