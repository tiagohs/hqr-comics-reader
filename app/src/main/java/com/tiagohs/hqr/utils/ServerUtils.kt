package com.tiagohs.hqr.utils

import android.content.Context
import android.net.ConnectivityManager

class ServerUtils {

    companion object {

        fun isNetworkConnected(context: Context?): Boolean {
            try {
                val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val isNetworkAvailable = cm.activeNetworkInfo != null
                return isNetworkAvailable && cm.activeNetworkInfo.isConnected
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

        }

        fun isWifiConnected(context: Context?): Boolean {
            val connManager = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val network = connManager.allNetworks
                if (network != null && network.size > 0) {
                    for (i in network.indices) {
                        val networkInfo = connManager.getNetworkInfo(network[i])
                        val networkType = networkInfo.type

                        if (ConnectivityManager.TYPE_WIFI == networkType)
                            return true
                    }
                }
            } else {
                val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                return mWifi.isConnected
            }

            return false
        }

    }
}