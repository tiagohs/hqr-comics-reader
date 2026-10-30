package com.tiagohs.hqr.data.update

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.domain.repository.UpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@Serializable
internal data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
internal data class AssetDto(
    val name: String,
    @SerialName("browser_download_url") val url: String,
)

/**
 * Reads `releases/latest` of the repository (RF-82, ADR-015): a version newer than this one, its notes, the APK and
 * the SHA-256 published next to it (`<apk>.sha256`). The automatic check runs at most once a day.
 */
class ReleaseChecker(
    private val http: HttpClient,
    private val settings: Settings,
    private val currentVersion: String,
    private val clock: Clock = Clock.System,
    private val url: String = LATEST,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(force: Boolean): UpdateInfo? {
        val now = clock.now().toEpochMilliseconds()
        if (!force && now - (settings.getLongOrNull(KEY_CHECKED) ?: 0L) < DAY_MS) return null
        val text = networkCall { http.get(url) { header("Accept", "application/vnd.github+json") }.ensureOk().bodyAsText() }
        settings.putLong(KEY_CHECKED, now)
        val release = json.decodeFromString(ReleaseDto.serializer(), text)
        val apk =
            release.assets
                .firstOrNull { it.name.endsWith(".apk") && it.url.startsWith("https://") }
                ?.takeIf { !release.draft && !release.prerelease && newer(release.tagName, currentVersion) }
                ?: return null
        val sha =
            release.assets.firstOrNull { it.name == apk.name + ".sha256" }?.let { asset ->
                networkCall { http.get(asset.url).ensureOk().bodyAsText() }
                    .trim()
                    .split(Regex("\\s+"))
                    .firstOrNull()
                    ?.lowercase()
            }
        return UpdateInfo(release.tagName.trimStart('v', 'V'), release.body.orEmpty().trim(), apk.url, sha)
    }

    companion object {
        const val LATEST = "https://api.github.com/repos/tiagohs/hqr-comics-reader/releases/latest"
        private const val KEY_CHECKED = "updates.checkedAt"
        private val DAY_MS = 24.hours.inWholeMilliseconds

        /** "v1.2.0" is newer than "1.1.9"; only the numbers count, part by part. */
        fun newer(
            tag: String,
            current: String,
        ): Boolean {
            val a = Regex("\\d+").findAll(tag).map { it.value.toInt() }.toList()
            val b = Regex("\\d+").findAll(current).map { it.value.toInt() }.toList()
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
