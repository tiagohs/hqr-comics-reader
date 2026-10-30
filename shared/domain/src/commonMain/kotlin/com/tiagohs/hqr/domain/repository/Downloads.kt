package com.tiagohs.hqr.domain.repository

import kotlinx.coroutines.flow.Flow

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
)

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
}

/** Starts the platform's executor for the queue (WorkManager on Android); a no-op where downloads don't run. */
fun interface DownloadScheduler {
    fun schedule()
}
