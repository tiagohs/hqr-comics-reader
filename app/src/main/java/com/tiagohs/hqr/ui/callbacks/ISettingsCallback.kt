package com.tiagohs.hqr.ui.callbacks

import android.content.Intent

interface ISettingsCallback {

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}