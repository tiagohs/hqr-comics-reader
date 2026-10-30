package com.tiagohs.hqr.presentation.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.ServerInput
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.ServerProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SCR-SRV-001: add or edit a server, "Testar" saying the cause of a failure (RF-17, RF-19, RF-95). */
class AddServerViewModel(
    private val servers: ServerManager,
    private val editing: SourceId? = null,
) : ViewModel() {
    data class State(
        val input: ServerInput = ServerInput("", ""),
        val busy: Boolean = false,
        /** The last check: null when not tested since the last change. */
        val result: Result? = null,
        val saved: Boolean = false,
        val isEdit: Boolean = false,
    ) {
        val canSubmit: Boolean get() = !busy && input.url.isNotBlank()
    }

    sealed interface Result {
        data object Ok : Result

        data class Problem(
            val problem: ServerProblem,
        ) : Result
    }

    private val _state = MutableStateFlow(State(isEdit = editing != null))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        if (editing != null) {
            viewModelScope.launch {
                // The password is never shown again: blank keeps nothing, so the user types it to change it.
                servers.observe().first().firstOrNull { it.id == editing }?.let { s ->
                    _state.update { it.copy(input = ServerInput(s.name, s.url, s.username.orEmpty())) }
                }
            }
        }
    }

    fun edit(input: ServerInput) {
        _state.update { it.copy(input = input, result = null) }
    }

    fun test() =
        run { input ->
            _state.update { it.copy(result = servers.test(input).toResult()) }
        }

    fun save() =
        run { input ->
            val problem = servers.save(input, editing)
            _state.update { it.copy(result = problem.toResult(), saved = problem == null) }
        }

    private fun run(block: suspend (ServerInput) -> Unit) {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(busy = true, result = null) }
        viewModelScope.launch {
            try {
                block(current.input)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun ServerProblem?.toResult(): Result = this?.let { Result.Problem(it) } ?: Result.Ok
}
