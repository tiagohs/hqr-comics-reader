package com.tiagohs.hqr.domain.repository

/** A newer release on GitHub (RF-82). */
data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sha256: String?,
)

/** RF-82, ADR-015: GitHub Releases, at most once a day on its own and whenever the user asks. */
interface Updates {
    /** The newer release, or null when this is the latest; [force] skips the once-a-day rule. */
    suspend fun check(force: Boolean): Result<UpdateInfo?>

    /** Downloads the APK, checks its SHA-256 and opens the system installer; [progress] goes 0..1. */
    suspend fun install(
        info: UpdateInfo,
        progress: (Float) -> Unit,
    ): Result<Unit>
}
