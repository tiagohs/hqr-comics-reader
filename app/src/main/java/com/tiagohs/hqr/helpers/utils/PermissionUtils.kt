package com.tiagohs.hqr.helpers.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.util.*

class PermissionUtils(val activity: Activity) {

    val REQUEST_PERMISSIONS = 12121
    var callback: PermissionsCallback? = null

    fun onCheckAndRequestPermissions(permissions: List<String>, callback: PermissionsCallback?) {
        this.callback = callback

        val permissionNotGranted = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED)
                permissionNotGranted.add(permission)
        }

        if (!permissionNotGranted.isEmpty()) {
            onShowNeedPermissionsDialog(permissions, callback)
        } else {
            callback?.onPermissionsGranted()
        }

    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        when (requestCode) {
            REQUEST_PERMISSIONS -> if (permissions.size > 0) {
                onChekPermissionRequested(requestCode, permissions, grantResults)
            } else {
                callback?.onPermissionsGranted()
            }
        }
    }

    fun onChekPermissionRequested(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val permissionNotGranted = ArrayList<String>()

        for (cont in permissions.indices) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val permission = permissions[0]

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    permissionNotGranted.add(permissions[0])

                } else {
                    callback?.onNeverAskAgain(requestCode)
                }
            }
        }

        if (!permissionNotGranted.isEmpty()) {
            //onCheckAndRequestPermissions(permissionNotGranted, callback)
        }
    }

    private fun onShowNeedPermissionsDialog(permissions: List<String>, callback: PermissionsCallback?) {
        ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), REQUEST_PERMISSIONS)
    }

}

interface PermissionsCallback {

    fun onPermissionsGranted()
    fun onPermissionsDenied()
    fun onNeverAskAgain(requestCode: Int)
}