package com.tiagohs.hqr.data.backup

import androidx.room3.withWriteTransaction
import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.CategoryEntity
import com.tiagohs.hqr.data.db.ChapterEntity
import com.tiagohs.hqr.data.db.HistoryEntity
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.MangaCategoryEntity
import com.tiagohs.hqr.data.db.MangaEntity
import com.tiagohs.hqr.data.db.MangaReaderSettingsEntity
import com.tiagohs.hqr.data.db.ProgressEntity
import com.tiagohs.hqr.data.db.ServerEntity
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.repository.Backups
import com.tiagohs.hqr.domain.repository.RestoreFailure
import com.tiagohs.hqr.domain.repository.RestoreProblem
import com.tiagohs.hqr.domain.repository.RestoreSummary
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** gzip for the backup file (ADR-010): java.util.zip on Android and the JVM. */
expect fun gzip(bytes: ByteArray): ByteArray

expect fun gunzip(bytes: ByteArray): ByteArray

/** The backup file was made by a newer HQR, or is not a backup at all. */
class BackupException(
    message: String,
    val newerVersion: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Creates and restores the `.hqrbackup` (RF-72 to RF-74, RF-89, ADR-010). Restoring only adds: chapters read are
 * joined, the larger progress stays, local work settings stay, all in one transaction.
 */
class BackupService(
    private val db: HqrDatabase,
    private val settings: Settings,
    private val app: String,
    /** Source id → name of every source this build has, built in or a server. */
    private val knownSources: () -> Map<String, String>,
    private val clock: Clock = Clock.System,
) : Backups {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    override suspend fun create(): ByteArray = gzip(json.encodeToString(Backup.serializer(), build()).encodeToByteArray())

    suspend fun build(): Backup {
        val mangas = db.mangaDao().all()
        val chapters = db.chapterDao().all().groupBy { it.mangaId }
        val progress = db.progressDao().all().associateBy { it.chapterId }
        val history = db.historyDao().all().associateBy { it.chapterId }
        val categories = db.categoryDao().list()
        val categoryNames = categories.associate { it.id to it.name }
        val assignments = db.categoryDao().allAssignments().groupBy({ it.mangaId }, { it.categoryId })
        val names = knownSources()
        val kept =
            mangas.filter { m ->
                m.inLibrary || chapters[m.id].orEmpty().any { progress[it.id] != null || history[it.id] != null }
            }
        return Backup(
            createdAtEpochMs = clock.now().toEpochMilliseconds(),
            app = app,
            categories = categories.map { BackupCategory(it.name, it.sortOrder) },
            mangas =
                kept.map { m ->
                    BackupManga(
                        sourceId = m.sourceId,
                        sourceName = names[m.sourceId] ?: m.sourceId,
                        key = m.key,
                        title = m.title,
                        url = m.url,
                        coverUrl = m.coverUrl,
                        inLibrary = m.inLibrary,
                        addedAtEpochMs = m.addedAt,
                        categories = assignments[m.id].orEmpty().mapNotNull { categoryNames[it] },
                        chapters =
                            chapters[m.id]
                                .orEmpty()
                                .filter { progress[it.id] != null || history[it.id] != null }
                                .map { c ->
                                    val p = progress[c.id]
                                    BackupChapter(
                                        c.key,
                                        c.number,
                                        c.title,
                                        p?.read ?: false,
                                        p?.page ?: 0,
                                        p?.pageCount,
                                        history[c.id]?.readAt,
                                    )
                                },
                        reader = db.readerSettingsDao().get(m.id)?.toBackup(),
                    )
                },
            servers = db.serverDao().list().map { BackupServer(it.sourceId, it.kind, it.name, it.url, it.username) },
            settings = SETTINGS.mapNotNull { (key, type) -> read(key, type)?.let { key to it } }.toMap(),
        )
    }

    override suspend fun restore(bytes: ByteArray): Result<RestoreSummary> =
        try {
            Result.success(restoreOrThrow(bytes))
        } catch (e: BackupException) {
            Result.failure(RestoreFailure(if (e.newerVersion) RestoreProblem.NEWER_VERSION else RestoreProblem.NOT_A_BACKUP))
        }

    override fun fileName(): String {
        val date = clock.now().toString().substringBefore('T')
        return "hqr-$date.hqrbackup"
    }

    suspend fun restoreOrThrow(bytes: ByteArray): RestoreSummary {
        val backup =
            try {
                json.decodeFromString(Backup.serializer(), gunzip(bytes).decodeToString())
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                throw BackupException("Não é um backup do HQR", cause = e)
            }
        if (backup.schemaVersion > Backup.SCHEMA_VERSION) throw BackupException("Backup de uma versão mais nova", newerVersion = true)
        val summary = db.withWriteTransaction { merge(backup) }
        // App settings: those already set on this device stay.
        backup.settings.forEach { (key, value) ->
            val type = SETTINGS[key] ?: return@forEach
            if (!settings.hasKey(key)) write(key, type, value)
        }
        return summary
    }

    private suspend fun merge(backup: Backup): RestoreSummary {
        val now = clock.now().toEpochMilliseconds()
        val categoryDao = db.categoryDao()
        val existing = categoryDao.list().associateBy { it.name.trim().lowercase() }.toMutableMap()
        var nextOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: 0)
        var newCategories = 0
        backup.categories.sortedBy { it.order }.forEach { c ->
            if (c.name.trim().lowercase() !in existing) {
                val id = categoryDao.insert(CategoryEntity(name = c.name.trim(), sortOrder = ++nextOrder))
                existing[c.name.trim().lowercase()] = CategoryEntity(id, c.name.trim(), nextOrder)
                newCategories++
            }
        }
        var read = 0
        backup.mangas.forEach { b ->
            val mangaId = upsertManga(b, now)
            b.categories.mapNotNull { existing[it.trim().lowercase()]?.id }.forEach {
                categoryDao.assign(MangaCategoryEntity(mangaId, it))
            }
            read += mergeChapters(mangaId, b.chapters, now)
            if (b.reader != null && db.readerSettingsDao().get(mangaId) == null) db.readerSettingsDao().upsert(b.reader.toEntity(mangaId))
        }
        val serverDao = db.serverDao()
        val savedServers = serverDao.list().map { it.sourceId }.toSet()
        val newServers = backup.servers.filter { it.sourceId !in savedServers }
        newServers.forEach { serverDao.upsert(ServerEntity(it.sourceId, it.kind, it.name, it.url, it.username, pinnedCertificate = null)) }
        val known = knownSources().keys + backup.servers.map { it.sourceId }
        return RestoreSummary(
            works = backup.mangas.size,
            chaptersRead = read,
            categories = newCategories,
            servers = newServers.size,
            unavailableSources =
                backup.mangas
                    .filter { it.sourceId !in known }
                    .map { it.sourceName }
                    .distinct()
                    .sorted(),
        )
    }

    private suspend fun upsertManga(
        b: BackupManga,
        now: Long,
    ): Long {
        val dao = db.mangaDao()
        val local = dao.find(b.sourceId, b.key)
        if (local == null) {
            return dao.insert(
                MangaEntity(
                    sourceId = b.sourceId,
                    key = b.key,
                    url = b.url,
                    title = b.title,
                    alternativeTitles = EMPTY_LIST,
                    coverUrl = b.coverUrl,
                    authors = EMPTY_LIST,
                    artists = EMPTY_LIST,
                    status = UNKNOWN,
                    genres = EMPTY_LIST,
                    language = null,
                    description = null,
                    kind = UNKNOWN,
                    inLibrary = b.inLibrary,
                    addedAt = if (b.inLibrary) b.addedAtEpochMs ?: now else null,
                    coverColor = null,
                    updatedAt = now,
                ),
            )
        }
        if (b.inLibrary && !local.inLibrary) dao.setInLibrary(local.id, true, local.addedAt ?: b.addedAtEpochMs ?: now)
        return local.id
    }

    /** RF-89: read stays read, the larger page wins, the latest reading time wins. Returns chapters read by the backup. */
    private suspend fun mergeChapters(
        mangaId: Long,
        chapters: List<BackupChapter>,
        now: Long,
    ): Int {
        val chapterDao = db.chapterDao()
        val progressDao = db.progressDao()
        val historyDao = db.historyDao()
        val localByKey = chapterDao.list(mangaId).associateBy { it.key }
        var read = 0
        chapters.forEach { b ->
            val id =
                localByKey[b.key]?.id ?: chapterDao.insert(
                    ChapterEntity(
                        mangaId = mangaId,
                        key = b.key,
                        number = b.number,
                        title = b.title,
                        volume = null,
                        language = null,
                        group = null,
                        groupUrl = null,
                        uploadedAt = null,
                        fetchedAt = now,
                        removedAtSource = false,
                        externalUrl = null,
                        sourceOrder = 0,
                    ),
                )
            val local = progressDao.get(id)
            if (b.read || b.page > 0) {
                progressDao.upsert(
                    ProgressEntity(
                        chapterId = id,
                        page = maxOf(local?.page ?: 0, b.page),
                        pageCount = local?.pageCount ?: b.pageCount,
                        read = (local?.read ?: false) || b.read,
                        updatedAt = maxOf(local?.updatedAt ?: 0, now),
                    ),
                )
            }
            if (b.read) read++
            b.readAtEpochMs?.let { at ->
                val previous = historyDao.all().firstOrNull { it.chapterId == id }?.readAt ?: 0
                if (at > previous) historyDao.upsert(HistoryEntity(id, at))
            }
        }
        return read
    }

    private fun read(
        key: String,
        type: Type,
    ): String? =
        when (type) {
            Type.BOOLEAN -> settings.getBooleanOrNull(key)?.toString()
            Type.INT -> settings.getIntOrNull(key)?.toString()
            Type.STRING -> settings.getStringOrNull(key)
        }

    private fun write(
        key: String,
        type: Type,
        value: String,
    ) {
        when (type) {
            Type.BOOLEAN -> value.toBooleanStrictOrNull()?.let { settings.putBoolean(key, it) }
            Type.INT -> value.toIntOrNull()?.let { settings.putInt(key, it) }
            Type.STRING -> settings.putString(key, value)
        }
    }

    private enum class Type { BOOLEAN, INT, STRING }

    private fun MangaReaderSettingsEntity.toBackup() = BackupReaderSettings(mode, scale, tapZones, background, doublePage, cropBorders)

    private fun BackupReaderSettings.toEntity(mangaId: Long) =
        MangaReaderSettingsEntity(mangaId, mode, scale, tapZones, background, doublePage, cropBorders)

    private companion object {
        const val EMPTY_LIST = "[]"
        const val UNKNOWN = "UNKNOWN"
        const val LIBRARY_OPTIONS = "library_options"

        // The app settings that travel (RF-72); never the local folder, never anything secret.
        val SETTINGS =
            mapOf(
                AppPreferences.THEME to Type.INT,
                AppPreferences.APP_LANGUAGE to Type.STRING,
                AppPreferences.PURE_BLACK to Type.BOOLEAN,
                AppPreferences.COVER_COLOR to Type.BOOLEAN,
                AppPreferences.LANGUAGES to Type.STRING,
                AppPreferences.WIFI_ONLY to Type.BOOLEAN,
                AppPreferences.DELETE_AFTER_READ to Type.BOOLEAN,
                AppPreferences.MANGADEX_SUGGESTIVE to Type.BOOLEAN,
                LIBRARY_OPTIONS to Type.STRING,
            )
    }
}
