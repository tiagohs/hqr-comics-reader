package com.tiagohs.hqr.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** One chapter in the download queue or already downloaded (SCR-DL-001). */
data class DownloadItem(
    val chapterId: Long,
    val mangaId: Long,
    val mangaTitle: String,
    val chapterLabel: String,
    val state: DownloadState,
    val pagesDone: Int,
    val pagesTotal: Int,
    val error: String? = null,
) {
    /** RF-68: paused because the device ran short of space. */
    val lowSpace: Boolean get() = state == DownloadState.PAUSED && error == LOW_SPACE

    companion object {
        /** The reason kept on the chapters the queue paused for lack of space (RF-68). */
        const val LOW_SPACE = "LOW_SPACE"

        /** RF-68: below this much free space, the queue pauses. */
        const val MIN_FREE_BYTES = 200L * 1024 * 1024
    }
}

enum class DownloadState { QUEUED, RUNNING, PAUSED, WAITING_WIFI, DONE, ERROR }

/**
 * The download queue (ADR-009): kept in the database, run by the platform's executor; files read by the reader even
 * without internet (RF-62 to RF-66).
 */
interface Downloads {
    fun observe(): Flow<List<DownloadItem>>

    /** RF-62: these chapters enter the end of the queue, in the order given; those already there stay. */
    suspend fun enqueue(chapterIds: List<Long>)

    suspend fun pauseAll()

    suspend fun resumeAll()

    /** RF-63: one place up (-1) or down (+1). */
    suspend fun move(
        chapterId: Long,
        by: Int,
    )

    /** RF-63, RF-66: out of the queue, or the files of a downloaded chapter removed. */
    suspend fun remove(chapterIds: Collection<Long>)

    suspend fun removeAll()

    /** RF-66: bytes of every downloaded chapter. */
    suspend fun usedBytes(): Long

    /** RF-64: "só no Wi-Fi" changed; chapters waiting for the network try again under the new rule. */
    suspend fun networkRuleChanged() {}

    /** RF-63: on opening the app, a queue left with work starts again. */
    suspend fun resumePending() {}

    /**
     * RF-66: these chapters may have just become read (marked by hand, or the reader left them at the end). With
     * "apagar depois de lido" on, the files of the read ones go; a chapter downloaded again after being read stays.
     */
    suspend fun chaptersRead(chapterIds: Collection<Long>) {}
}

/** Starts the platform's executor for the queue (WorkManager on Android); a no-op where downloads don't run. */
fun interface DownloadScheduler {
    /** [restart]: drop a run waiting for the old network rule and start one with the current rule (RF-64). */
    fun schedule(restart: Boolean)
}

/** The two download options of SCR-DL-001: Wi-Fi only (RF-64) and delete after read (RF-66). */
interface DownloadSettings {
    val wifiOnly: StateFlow<Boolean>
    val deleteAfterRead: StateFlow<Boolean>

    /** RF-67: each finished chapter becomes one `.cbz`. */
    val saveAsCbz: StateFlow<Boolean>

    /** RF-67: the folder that also gets a `.cbz` of each finished chapter; null keeps them only in the app. */
    val folder: StateFlow<String?>

    fun setSaveAsCbz(on: Boolean)

    fun setFolder(uri: String?)

    fun setWifiOnly(on: Boolean)

    fun setDeleteAfterRead(on: Boolean)
}

/** RF-87: whether the device has a network now; without one, nothing online is asked and nothing changes. */
interface Connectivity {
    val online: StateFlow<Boolean>

    /** Where the platform can't tell (tests, desktop), the network is assumed. */
    object Always : Connectivity {
        override val online: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true)
    }
}
