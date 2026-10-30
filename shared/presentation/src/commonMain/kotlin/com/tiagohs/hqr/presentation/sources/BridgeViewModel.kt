package com.tiagohs.hqr.presentation.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.source.Bridge
import com.tiagohs.hqr.domain.source.BridgeCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SCR-WEB-001: address and token of the user's bridge; "Testar" first, saved only when it answers (RF-108). */
class BridgeViewModel(
    private val bridge: Bridge,
) : ViewModel() {
    data class State(
        val address: String = "",
        val token: String = "",
        val testing: Boolean = false,
        val check: BridgeCheck? = null,
        val saved: String? = null,
    )

    private val _state = MutableStateFlow(State(address = bridge.address.value.orEmpty(), saved = bridge.address.value))
    val state: StateFlow<State> = _state.asStateFlow()

    fun setAddress(value: String) = _state.update { it.copy(address = value, check = null) }

    fun setToken(value: String) = _state.update { it.copy(token = value, check = null) }

    /** Tests and, when the bridge answers, saves. */
    fun test() {
        val s = _state.value
        _state.update { it.copy(testing = true, check = null) }
        viewModelScope.launch {
            val check = bridge.test(s.address, s.token)
            // An outdated bridge still serves the older sources: it is kept, with the warning on screen.
            if (check == BridgeCheck.OK || check == BridgeCheck.OUTDATED) bridge.save(s.address, s.token)
            _state.update { it.copy(testing = false, check = check, saved = bridge.address.value) }
        }
    }

    fun remove() {
        viewModelScope.launch {
            bridge.clear()
            _state.value = State()
        }
    }
}
