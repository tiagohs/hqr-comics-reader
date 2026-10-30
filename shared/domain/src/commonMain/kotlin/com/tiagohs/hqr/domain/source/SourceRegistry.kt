package com.tiagohs.hqr.domain.source

import com.tiagohs.hqr.domain.model.SourceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The sources this build carries (RF-02, ADR-003), plus the user's servers registered at runtime (RF-17).
 * A source the user turned off leaves Explore and the searches but its works still open (RF-03); one the remote list
 * turned off is unavailable everywhere (RF-105).
 */
class SourceRegistry(
    /** The sources that come with the app (the user's servers are added later). */
    val builtIn: List<Source>,
    ready: Boolean = true,
) {
    // The user's servers and switches come from the database at start: lookups wait for them (RF-88).
    private val readyFlag = MutableStateFlow(ready)

    /** True once the saved servers and switches are applied: before that, a missing source may still come. */
    val isReady: StateFlow<Boolean> = readyFlag.asStateFlow()

    fun markReady() {
        readyFlag.value = true
    }

    /** Waits until the saved servers and switches are applied (at most [READY_WAIT_MS]). */
    suspend fun awaitReady() {
        withTimeoutOrNull(READY_WAIT_MS) { readyFlag.first { it } }
    }

    /** [require] after the start: a server's work opened right after the process returns finds its source. */
    suspend fun awaitRequire(id: SourceId): Source {
        awaitReady()
        return require(id)
    }

    private val sources = LinkedHashMap<SourceId, Source>().apply { builtIn.forEach { put(it.id, it) } }
    private var userOff: Set<SourceId> = emptySet()
    private var listOff: Set<SourceId> = emptySet()
    private val _active = MutableStateFlow<List<Source>>(emptyList())
    private val _version = MutableStateFlow(0)

    /** Sources on for Explore, global search and the feed, in order. */
    val active: StateFlow<List<Source>> = _active.asStateFlow()

    /** Grows on every change of sources or switches: screens that list everything watch it. */
    val version: StateFlow<Int> = _version.asStateFlow()

    init {
        refresh()
    }

    /** Every source the build knows, on or off, except those the remote list turned off. */
    val all: List<Source> get() = sources.values.filter { it.id !in listOff }

    /** Including the ones the remote list turned off, for the sources screen. */
    val everything: List<Source> get() = sources.values.toList()

    operator fun get(id: SourceId): Source? = sources[id]?.takeIf { id !in listOff }

    fun require(id: SourceId): Source = get(id) ?: throw SourceException("Fonte indisponível: $id", SourceException.Kind.UNAVAILABLE)

    fun status(id: SourceId): Status =
        when (id) {
            in listOff -> Status.OFF_BY_LIST
            in userOff -> Status.OFF_BY_USER
            else -> Status.ON
        }

    fun register(source: Source) {
        sources[source.id] = source
        refresh()
    }

    fun unregister(id: SourceId) {
        sources.remove(id)
        refresh()
    }

    /** RF-03: the switches of the sources screen. */
    fun setUserDisabled(ids: Set<SourceId>) {
        userOff = ids
        refresh()
    }

    /** RF-105: the sources the remote list turned off. */
    fun setListDisabled(ids: Set<SourceId>) {
        listOff = ids
        refresh()
    }

    private fun refresh() {
        _active.value = sources.values.filter { it.id !in userOff && it.id !in listOff }
        _version.value += 1
    }

    enum class Status { ON, OFF_BY_USER, OFF_BY_LIST }

    private companion object {
        const val READY_WAIT_MS = 10_000L
    }
}

/** RF-03: turns a source on or off for Explore and the searches, and remembers it. */
fun interface SourceSwitches {
    suspend fun setEnabled(
        id: SourceId,
        enabled: Boolean,
    )

    /** RF-73: the name kept for a source this app doesn't have (read from a restored backup). */
    suspend fun savedName(id: SourceId): String? = null
}

/** Bytes of a page or cover, from the cache, the downloads or the source (ADR-006). */
fun interface ImageBytes {
    suspend fun load(
        sourceId: SourceId,
        url: String,
        headers: Map<String, String>,
    ): ByteArray
}
