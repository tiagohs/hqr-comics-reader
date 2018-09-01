package com.tiagohs.hqr.helpers.extensions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import com.tiagohs.hqr.BuildConfig
import java.io.File

fun File.getUriCompat(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", this)
    else Uri.fromFile(this)
}

