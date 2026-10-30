package com.tiagohs.hqr.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadSettings
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.repository.Downloads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** SCR-DL-001: the queue (pause, resume, order, cancel) and what is downloaded, with the space used (RF-62 to RF-66). */
class DownloadsViewModel(
    private val downloads: Downloads,
    private val settings: DownloadSettings,
) : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val queue: List<DownloadItem> = emptyList(),
        val done: List<DownloadItem> = emptyList(),
        val usedBytes: Long = 0,
        val wifiOnly: Boolean = true,
        val deleteAfterRead: Boolean = false,
        val saveAsCbz: Boolean = false,
        /** RF-67: the folder that gets a copy of each chapter; null when none. */
        val folder: String? = null,
    ) {
        /** RF-68: the queue was paused for lack of space. */
        val lowSpace: Boolean get() = queue.any { it.lowSpace }

        val paused: Boolean get() = queue.isNotEmpty() && queue.all { it.state == DownloadState.PAUSED || it.state == DownloadState.ERROR }
        val isEmpty: Boolean get() = !loading && queue.isEmpty() && done.isEmpty()
    }

    private val used = MutableStateFlow(0L)

    val state: StateFlow<State> =
        combine(
            combine(downloads.observe(), used, settings.wifiOnly, settings.deleteAfterRead) { items, bytes, wifi, delete ->
                State(
                    loading = false,
                    queue = items.filter { it.state != DownloadState.DONE },
                    done = items.filter { it.state == DownloadState.DONE },
                    usedBytes = bytes,
                    wifiOnly = wifi,
                    deleteAfterRead = delete,
                )
            },
            settings.saveAsCbz,
            settings.folder,
        ) { state, cbz, folder -> state.copy(saveAsCbz = cbz, folder = folder) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), State())

    init {
        // The space follows what is downloaded.
        viewModelScope.launch { downloads.observe().collect { used.value = downloads.usedBytes() } }
    }

    fun pauseAll() = launch { downloads.pauseAll() }

    fun resumeAll() = launch { downloads.resumeAll() }

    fun move(
        chapterId: Long,
        by: Int,
    ) = launch { downloads.move(chapterId, by) }

    /** RF-63 cancel, RF-66 delete by chapter. */
    fun remove(chapterId: Long) = launch { downloads.remove(listOf(chapterId)) }

    /** RF-66: by work. */
    fun removeWork(mangaId: Long) =
        launch {
            downloads.remove(
                state.value.done
                    .filter { it.mangaId == mangaId }
                    .map { it.chapterId },
            )
        }

    /** RF-66: all. */
    fun removeAll() = launch { downloads.removeAll() }

    /** RF-64: chapters waiting for Wi-Fi try again under the new rule. */
    fun setWifiOnly(on: Boolean) {
        settings.setWifiOnly(on)
        viewModelScope.launch { downloads.networkRuleChanged() }
    }

    fun setDeleteAfterRead(on: Boolean) = settings.setDeleteAfterRead(on)

    /** RF-67. */
    fun setSaveAsCbz(on: Boolean) = settings.setSaveAsCbz(on)

    /** RF-67: null goes back to keeping the chapters only in the app. */
    fun setFolder(uri: String?) = settings.setFolder(uri)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
