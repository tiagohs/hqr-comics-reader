package com.tiagohs.hqr.web

import com.tiagohs.hqr.domain.repository.Connectivity
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** RF-87 in the browser: navigator.onLine and its online/offline events. */
class BrowserConnectivity : Connectivity {
    private val state = MutableStateFlow(window.navigator.onLine)
    override val online: StateFlow<Boolean> = state

    init {
        window.addEventListener("online") { state.value = true }
        window.addEventListener("offline") { state.value = false }
    }
}
