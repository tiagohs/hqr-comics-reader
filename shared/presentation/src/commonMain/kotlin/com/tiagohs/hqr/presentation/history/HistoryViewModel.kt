package com.tiagohs.hqr.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** SCR-HIS-001: readings grouped by day (RF-59), remove one and clear all (RF-60). */
class HistoryViewModel(
    private val history: HistoryRepository,
    private val clock: Clock,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    data class Day(
        val date: LocalDate,
        val isToday: Boolean,
        val isYesterday: Boolean,
        val entries: List<HistoryEntry>,
    )

    data class State(
        val loading: Boolean = true,
        val days: List<Day> = emptyList(),
    ) {
        val isEmpty: Boolean get() = !loading && days.isEmpty()
    }

    val state: StateFlow<State> =
        history
            .observe()
            .map { entries ->
                val today = clock.now().toLocalDateTime(zone).date
                val days =
                    entries
                        .groupBy { Instant.fromEpochMilliseconds(it.readAtEpochMs).toLocalDateTime(zone).date }
                        .map { (date, list) -> Day(date, date == today, date.toEpochDays() == today.toEpochDays() - 1, list) }
                        .sortedByDescending { it.date }
                State(false, days)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun remove(entry: HistoryEntry) {
        viewModelScope.launch { history.remove(entry.chapter.id) }
    }

    fun clearAll() {
        viewModelScope.launch { history.clear() }
    }
}
