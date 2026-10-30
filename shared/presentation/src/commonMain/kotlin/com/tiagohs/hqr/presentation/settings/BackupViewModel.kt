package com.tiagohs.hqr.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.repository.Backups
import com.tiagohs.hqr.domain.repository.RestoreFailure
import com.tiagohs.hqr.domain.repository.RestoreProblem
import com.tiagohs.hqr.domain.repository.RestoreSummary
import com.tiagohs.hqr.presentation.common.attempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SCR-SET-002: create a backup into a file the user picks, restore one and show what came in (RF-72, RF-73). */
class BackupViewModel(
    private val backups: Backups,
) : ViewModel() {
    data class State(
        val busy: Boolean = false,
        /** The bytes waiting for the file picker to give a place. */
        val pending: ByteArray? = null,
        val saved: Boolean? = null,
        val summary: RestoreSummary? = null,
        val problem: RestoreProblem? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val fileName: String get() = backups.fileName()

    /** Builds the file; the screen then asks where to save [State.pending]. */
    fun create() {
        _state.update { it.copy(busy = true, saved = null) }
        viewModelScope.launch {
            // Where the platform can't build the file (the browser for now), the screen says it was not saved.
            attempt { backups.create() }
                .onSuccess { bytes -> _state.update { it.copy(busy = false, pending = bytes) } }
                .onFailure { _state.update { it.copy(busy = false, saved = false) } }
        }
    }

    fun saved(ok: Boolean) = _state.update { it.copy(pending = null, saved = ok) }

    fun restore(bytes: ByteArray?) {
        if (bytes == null) return
        _state.update { it.copy(busy = true, summary = null, problem = null) }
        viewModelScope.launch {
            backups
                .restore(bytes)
                .onSuccess { s -> _state.update { it.copy(busy = false, summary = s) } }
                .onFailure { e ->
                    val problem = (e as? RestoreFailure)?.problem ?: RestoreProblem.NOT_A_BACKUP
                    _state.update { it.copy(busy = false, problem = problem) }
                }
        }
    }

    fun dismiss() = _state.update { it.copy(summary = null, problem = null, saved = null) }
}
