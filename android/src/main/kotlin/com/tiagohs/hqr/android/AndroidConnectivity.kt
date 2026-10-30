package com.tiagohs.hqr.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.tiagohs.hqr.domain.repository.Connectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** RF-87: follows the default network; a home Wi-Fi without internet still counts, so the user's servers work. */
class AndroidConnectivity(
    context: Context,
) : Connectivity {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(manager.activeNetwork != null)
    override val online: StateFlow<Boolean> = state

    init {
        manager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    state.value = true
                }

                override fun onLost(network: Network) {
                    state.value = manager.activeNetwork != null && manager.activeNetwork != network
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    caps: NetworkCapabilities,
                ) {
                    state.value = true
                }
            },
        )
    }
}
