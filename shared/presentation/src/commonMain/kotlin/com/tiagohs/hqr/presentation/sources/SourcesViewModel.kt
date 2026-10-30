package com.tiagohs.hqr.presentation.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** SCR-SRC-001: every source with its switch (RF-03) and why it is off when the remote list turned it off (RF-105). */
class SourcesViewModel(
    private val registry: SourceRegistry,
    private val switches: SourceSwitches,
    private val servers: ServerManager? = null,
) : ViewModel() {
    data class Row(
        val id: SourceId,
        val name: String,
        val languages: List<String>,
        val status: SourceRegistry.Status,
    ) {
        val on: Boolean get() = status == SourceRegistry.Status.ON
    }

    data class State(
        val online: List<Row> = emptyList(),
        val local: Row? = null,
        val servers: List<Server> = emptyList(),
    )

    val state: StateFlow<State> =
        combine(registry.version, servers?.observe() ?: flowOf(emptyList())) { _, saved ->
            val ids = saved.map { it.id }.toSet()
            val rows = registry.everything.filter { it.id !in ids }.map { Row(it.id, it.name, it.languages, registry.status(it.id)) }
            State(online = rows.filter { it.id != SourceId.LOCAL }, local = rows.firstOrNull { it.id == SourceId.LOCAL }, servers = saved)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), State())

    fun deleteServer(id: SourceId) {
        viewModelScope.launch { servers?.delete(id) }
    }

    /** A source the remote list turned off can't be turned on here. */
    fun setEnabled(
        id: SourceId,
        enabled: Boolean,
    ) {
        if (registry.status(id) == SourceRegistry.Status.OFF_BY_LIST) return
        viewModelScope.launch { switches.setEnabled(id, enabled) }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
