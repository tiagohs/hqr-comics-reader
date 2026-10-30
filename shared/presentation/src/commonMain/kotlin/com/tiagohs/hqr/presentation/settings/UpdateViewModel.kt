package com.tiagohs.hqr.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.repository.UpdateFailure
import com.tiagohs.hqr.domain.repository.UpdateInfo
import com.tiagohs.hqr.domain.repository.UpdateProblem
import com.tiagohs.hqr.domain.repository.Updates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SCR-SET-004: check, read the notes, download and install (RF-82). */
class UpdateViewModel(
    private val updates: Updates,
) : ViewModel() {
    data class State(
        val checking: Boolean = false,
        val checked: Boolean = false,
        val info: UpdateInfo? = null,
        val checkFailed: Boolean = false,
        val progress: Float? = null,
        val installError: UpdateProblem? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** [force]: the user asked; otherwise at most once a day. */
    fun check(force: Boolean = true) {
        _state.update { it.copy(checking = true, checkFailed = false) }
        viewModelScope.launch {
            updates
                .check(force)
                .onSuccess { info -> _state.update { it.copy(checking = false, checked = true, info = info) } }
                .onFailure { _state.update { it.copy(checking = false, checkFailed = true) } }
        }
    }

    fun install() {
        val info = _state.value.info ?: return
        _state.update { it.copy(progress = 0f, installError = null) }
        viewModelScope.launch {
            updates
                .install(info) { p -> _state.update { it.copy(progress = p) } }
                .onSuccess { _state.update { it.copy(progress = null) } }
                .onFailure { e ->
                    val problem = (e as? UpdateFailure)?.problem ?: UpdateProblem.NETWORK
                    _state.update { it.copy(progress = null, installError = problem) }
                }
        }
    }

    /** "Depois" on the automatic notice. */
    fun dismiss() = _state.update { it.copy(info = null) }
}
