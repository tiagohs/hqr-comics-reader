package com.tiagohs.hqr.domain.repository

/** What a restore did (RF-73): the summary and the works whose source this app doesn't have. */
data class RestoreSummary(
    val works: Int,
    val chaptersRead: Int,
    val categories: Int,
    val servers: Int,
    /** Source names read from the backup, for works this app can't open. */
    val unavailableSources: List<String>,
)

/** Why a file could not be restored. */
enum class RestoreProblem { NOT_A_BACKUP, NEWER_VERSION }

/** The `.hqrbackup` file (RF-72 to RF-74, RF-89). */
interface Backups {
    suspend fun create(): ByteArray

    /** Adds what the file has; nothing is deleted. */
    suspend fun restore(bytes: ByteArray): Result<RestoreSummary>

    /** The name offered when saving: hqr-2026-10-30.hqrbackup. */
    fun fileName(): String
}

/** The [Result] failure of [Backups.restore]. */
class RestoreFailure(
    val problem: RestoreProblem,
    cause: Throwable? = null,
) : Exception(problem.name, cause)
