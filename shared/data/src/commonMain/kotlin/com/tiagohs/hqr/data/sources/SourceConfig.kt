package com.tiagohs.hqr.data.sources

import androidx.room3.withWriteTransaction
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.SourceEntity
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.Source
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/** Reads the remote `sources.json`; null when it can't be read (no network, not published yet). */
fun interface SourceListFetcher {
    suspend fun fetch(): String?
}

@Serializable
data class SourceList(
    val version: Int = 1,
    val sources: List<Entry> = emptyList(),
) {
    @Serializable
    data class Entry(
        val id: String,
        val enabled: Boolean = true,
        val baseUrl: String? = null,
        val minAppVersion: String? = null,
    )
}

/**
 * The switches of the sources (RF-03, saved in the `source` table) and the remote list (RF-105, ADR-019): read once a
 * day, the last good copy kept; without any, the list built into the app.
 */
class SourceConfig(
    private val db: HqrDatabase,
    private val settings: Settings,
    private val registry: SourceRegistry,
    private val fetcher: SourceListFetcher,
    private val appVersion: String,
    private val clock: Clock = Clock.System,
) : SourceSwitches {
    private val dao = db.sourceDao()
    private val json = Json { ignoreUnknownKeys = true }
    private var baseUrls: Map<SourceId, String> = emptyMap()

    override suspend fun savedName(id: SourceId): String? = dao.list().firstOrNull { it.id == id.value }?.name

    /** RF-105: the address the list gives a source, if any. */
    fun baseUrl(id: SourceId): String? = baseUrls[id]

    /** Applies what is saved, then asks for a fresh list when the last one is older than a day. */
    suspend fun start() {
        applySaved()
        refreshIfStale()
    }

    /** The switches and the last list, from the device only: quick, done before any screen reads a source. */
    suspend fun applySaved() {
        migrateKeys()
        val off =
            dao
                .list()
                .filter { !it.enabled }
                .map { SourceId(it.id) }
                .toSet()
        registry.setUserDisabled(off)
        apply(settings.getStringOrNull(KEY_LIST) ?: EMBEDDED)
    }

    /** RF-105: once a day. */
    suspend fun refreshIfStale() {
        val last = settings.getLongOrNull(KEY_FETCHED_AT) ?: 0L
        if (clock.now().toEpochMilliseconds() - last >= DAY_MS) refresh()
    }

    /** Asks for the list now; a failure keeps the last one. */
    suspend fun refresh() {
        val text =
            try {
                fetcher.fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
            ) {
                null
            } ?: return
        if (!apply(text)) return
        settings.putString(KEY_LIST, text)
        settings.putLong(KEY_FETCHED_AT, clock.now().toEpochMilliseconds())
    }

    /**
     * RF-91: a source whose key format grew since the keys were saved gets them rewritten, works and chapters, before any
     * screen reads them. A key the source can't migrate, or that would clash with one already there, stays as it was.
     */
    suspend fun migrateKeys() =
        db.withWriteTransaction {
            // One transaction: a start killed halfway migrates nothing twice.
            migrateKeysUnlocked()
        }

    private suspend fun migrateKeysUnlocked() {
        val saved = dao.list().associateBy { it.id }
        // The servers' keys are addresses on the user's own server: only the app's sources change format.
        registry.builtIn.forEach { source ->
            val row = saved[source.id.value]
            val from = row?.schemaVersion ?: source.schemaVersion
            if (from < source.schemaVersion) rewriteKeys(source, from)
            when {
                row == null ->
                    dao.upsert(
                        SourceEntity(
                            source.id.value,
                            KIND_BUILT_IN,
                            source.name,
                            source.languages.joinToString(","),
                            source.schemaVersion,
                            true,
                            "{}",
                        ),
                    )
                row.schemaVersion < source.schemaVersion -> dao.upsert(row.copy(schemaVersion = source.schemaVersion))
                else -> Unit
            }
        }
    }

    private suspend fun rewriteKeys(
        source: Source,
        from: Int,
    ) {
        dao.mangaKeys(source.id.value).forEach { (id, key) ->
            source.migrateMangaKey(key, from)?.takeIf { it != key }?.let { new -> rewrite { dao.setMangaKey(id, new) } }
        }
        dao.chapterKeys(source.id.value).forEach { (id, key) ->
            source.migrateChapterKey(key, from)?.takeIf { it != key }?.let { new -> rewrite { dao.setChapterKey(id, new) } }
        }
    }

    // A clash with a key already saved (unique index) leaves this one as it was.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun rewrite(update: suspend () -> Unit) {
        try {
            update()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Unit
        }
    }

    /** RF-03: the switch of one source. */
    override suspend fun setEnabled(
        id: SourceId,
        enabled: Boolean,
    ) {
        val source = registry.everything.firstOrNull { it.id == id } ?: return
        val current = dao.list().firstOrNull { it.id == id.value }
        dao.upsert(
            current?.copy(enabled = enabled)
                ?: SourceEntity(
                    id.value,
                    KIND_BUILT_IN,
                    source.name,
                    source.languages.joinToString(","),
                    source.schemaVersion,
                    enabled,
                    "{}",
                ),
        )
        registry.setUserDisabled(
            dao
                .list()
                .filter { !it.enabled }
                .map { SourceId(it.id) }
                .toSet(),
        )
    }

    /** False when the text is not a list (the last good one stays). */
    private fun apply(text: String): Boolean {
        val list = runCatching { json.decodeFromString<SourceList>(text) }.getOrNull() ?: return false
        val off =
            list.sources
                .filter { !it.enabled || (it.minAppVersion != null && olderThan(appVersion, it.minAppVersion)) }
                .map { SourceId(it.id) }
                .toSet()
        registry.setListDisabled(off)
        baseUrls = list.sources.mapNotNull { e -> e.baseUrl?.takeIf { it.startsWith("https://") }?.let { SourceId(e.id) to it } }.toMap()
        return true
    }

    companion object {
        const val URL = "https://raw.githubusercontent.com/tiagohs/hqr-comics-reader/master/sources.json"
        private const val KIND_BUILT_IN = "builtin"
        private const val KEY_LIST = "sources.list"
        private const val KEY_FETCHED_AT = "sources.fetchedAt"
        private val DAY_MS = 24.hours.inWholeMilliseconds

        /** Same text as `sources.json` at the root of the repository (a test keeps them equal). */
        const val EMBEDDED = """{
  "version": 1,
  "sources": [
    { "id": "mangadex", "enabled": true },
    { "id": "hqnow", "enabled": true, "baseUrl": "https://admin.hq-now.com/graphql" },
    { "id": "local", "enabled": true }
  ]
}
"""

        /** "1.2.0" < "1.10.0": numbers compared part by part. */
        fun olderThan(
            version: String,
            minimum: String,
        ): Boolean {
            val a = version.split('.').map { it.toIntOrNull() ?: 0 }
            val b = minimum.split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x < y
            }
            return false
        }
    }
}

/** The published `sources.json` (raw GitHub, which also answers browsers: CORS open). */
class HttpSourceListFetcher(
    private val http: io.ktor.client.HttpClient,
    private val url: String = SourceConfig.URL,
) : SourceListFetcher {
    override suspend fun fetch(): String? {
        val response = http.get(url)
        return if (response.status.isSuccess()) response.bodyAsText() else null
    }
}
