package com.tiagohs.hqr.data.sync

import com.russhwolf.settings.Settings
import com.tiagohs.hqr.data.db.CategoryEntity
import com.tiagohs.hqr.data.db.ChapterEntity
import com.tiagohs.hqr.data.db.FeedItemEntity
import com.tiagohs.hqr.data.db.HistoryEntity
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.MangaCategoryEntity
import com.tiagohs.hqr.data.db.MangaEntity
import com.tiagohs.hqr.data.db.MangaReaderSettingsEntity
import com.tiagohs.hqr.data.db.ProgressEntity
import com.tiagohs.hqr.data.db.SavedSearchEntity
import com.tiagohs.hqr.data.db.ServerEntity
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.Shelf
import com.tiagohs.hqr.domain.sync.CategoryRecord
import com.tiagohs.hqr.domain.sync.ChapterRecord
import com.tiagohs.hqr.domain.sync.FeedRecord
import com.tiagohs.hqr.domain.sync.MangaRecord
import com.tiagohs.hqr.domain.sync.ReaderRecord
import com.tiagohs.hqr.domain.sync.SavedSearchRecord
import com.tiagohs.hqr.domain.sync.ServerRecord
import com.tiagohs.hqr.domain.sync.SyncChange
import com.tiagohs.hqr.domain.sync.SyncItemId
import com.tiagohs.hqr.domain.sync.SyncKeys
import com.tiagohs.hqr.domain.sync.SyncKind
import com.tiagohs.hqr.domain.sync.SyncRules
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The account's data as sync items, read from and written to the database and the preferences (RF-121). Works and
 * chapters by source and key (RF-90); never downloads, files, cache or device settings.
 */
@Suppress("TooManyFunctions") // One reader and one writer per kind.
class SyncLocalData(
    private val db: HqrDatabase,
    private val settings: Settings,
    private val prefs: AppPreferences,
) {
    /** Every item as this device has it now; chapters keep the user's unread mark from [previous]. */
    suspend fun current(previous: Map<SyncItemId, String>): Map<SyncItemId, String> {
        val items = LinkedHashMap<SyncItemId, String>()
        val categories = db.categoryDao().list()
        categories.forEach {
            items[SyncItemId(SyncKind.CATEGORY, categoryKey(it.name))] =
                encode(CategoryRecord.serializer(), CategoryRecord(it.name, it.sortOrder))
        }
        works(previous, categories.associate { it.id to it.name }, items)
        val saved = db.searchDao().listSaved()
        saved.forEach {
            items[SyncItemId(SyncKind.SAVED_SEARCH, SyncKeys.join(it.sourceId, it.name))] =
                encode(SavedSearchRecord.serializer(), SavedSearchRecord(it.query, it.filters))
        }
        db.searchDao().listFeed().forEach { f ->
            val search = f.savedSearchId?.let { id -> saved.firstOrNull { it.id == id }?.name ?: return@forEach }
            items[SyncItemId(SyncKind.FEED, SyncKeys.join(f.sourceId, search.orEmpty()))] =
                encode(FeedRecord.serializer(), FeedRecord(f.sortOrder))
        }
        db.serverDao().list().forEach {
            items[SyncItemId(SyncKind.SERVER, it.sourceId)] =
                encode(ServerRecord.serializer(), ServerRecord(it.kind, it.name, it.url, it.username))
        }
        SETTINGS.forEach { key -> readSetting(key)?.let { items[SyncItemId(SyncKind.SETTING, key)] = encode(String.serializer(), it) } }
        return items
    }

    /** The works and their read chapters. */
    private suspend fun works(
        previous: Map<SyncItemId, String>,
        categoryNames: Map<Long, String>,
        items: MutableMap<SyncItemId, String>,
    ) {
        val chapters = db.chapterDao().all().groupBy { it.mangaId }
        val progress = db.progressDao().all().associateBy { it.chapterId }
        val history = db.historyDao().all().associateBy { it.chapterId }
        val assignments = db.categoryDao().allAssignments().groupBy({ it.mangaId }, { it.categoryId })
        db.mangaDao().all().forEach { m ->
            val id = SyncItemId(SyncKind.MANGA, SyncKeys.join(m.sourceId, m.key))
            val read = chapters[m.id].orEmpty().filter { progress[it.id] != null || history[it.id] != null }
            // A work the account already knows stays an item even out of the library, so it never turns into a tombstone.
            if (!m.inLibrary && read.isEmpty() && id !in previous) return@forEach
            // Title, address and cover only matter where the work is new: the account's first version is kept, so a
            // source renaming a work on one device doesn't bounce between devices.
            val known = previous[id]?.let { decodeOrNull(MangaRecord.serializer(), it) }
            val record =
                MangaRecord(
                    title = known?.title ?: m.title,
                    url = known?.url ?: m.url,
                    coverUrl = known?.coverUrl ?: m.coverUrl,
                    inLibrary = m.inLibrary,
                    addedAt = m.addedAt,
                    categories = assignments[m.id].orEmpty().mapNotNull { categoryNames[it] }.sorted(),
                    reader = db.readerSettingsDao().get(m.id)?.toRecord(),
                    shelf = m.shelf,
                )
            items[id] = encode(MangaRecord.serializer(), record)
            read.forEach { c ->
                val chapterId = SyncItemId(SyncKind.CHAPTER, SyncKeys.join(m.sourceId, m.key, c.key))
                items[chapterId] = chapterRecord(c, progress[c.id], history[c.id], previous[chapterId])
            }
        }
    }

    private fun chapterRecord(
        chapter: ChapterEntity,
        progress: ProgressEntity?,
        history: HistoryEntity?,
        previous: String?,
    ): String {
        val before = previous?.let { decodeOrNull(ChapterRecord.serializer(), it) }
        val now =
            ChapterRecord(
                number = before?.number ?: chapter.number,
                title = before?.title ?: chapter.title,
                read = progress?.read ?: false,
                page = progress?.page ?: 0,
                pageCount = progress?.pageCount,
                progressAt = progress?.updatedAt ?: 0,
                readAt = history?.readAt,
            )
        return encode(ChapterRecord.serializer(), SyncRules.withUnreadMark(before, now))
    }

    /**
     * Writes what came from the account. Chapters merge with [local] (this device's items after the last capture);
     * the other kinds take the account's version, which already is the newest (RF-123). Returns how many works
     * entered the library (RF-124).
     */
    suspend fun apply(
        changes: List<SyncChange>,
        local: Map<SyncItemId, String>,
        now: Long,
    ): Int {
        var arrived = 0
        val byKind = changes.groupBy { it.kind }
        byKind[SyncKind.CATEGORY].orEmpty().forEach { applyCategory(it) }
        byKind[SyncKind.MANGA].orEmpty().forEach { if (applyManga(it, now)) arrived++ }
        byKind[SyncKind.CHAPTER].orEmpty().forEach { applyChapter(it, local[it.id], now) }
        byKind[SyncKind.SAVED_SEARCH].orEmpty().forEach { applySavedSearch(it) }
        byKind[SyncKind.FEED].orEmpty().forEach { applyFeed(it) }
        byKind[SyncKind.SERVER].orEmpty().forEach { applyServer(it) }
        byKind[SyncKind.SETTING].orEmpty().forEach { applySetting(it) }
        return arrived
    }

    private suspend fun applyCategory(change: SyncChange) {
        val dao = db.categoryDao()
        val existing = dao.list().firstOrNull { categoryKey(it.name) == change.key }
        if (change.deleted) {
            existing?.let { dao.delete(it.id) }
            return
        }
        val record = decodeOrNull(CategoryRecord.serializer(), change.payload) ?: return
        if (existing == null) {
            dao.insert(CategoryEntity(name = record.name, sortOrder = record.order))
        } else {
            if (existing.name != record.name) dao.rename(existing.id, record.name)
            if (existing.sortOrder != record.order) dao.setOrder(existing.id, record.order)
        }
    }

    /** True when the work entered the library here. */
    private suspend fun applyManga(
        change: SyncChange,
        now: Long,
    ): Boolean {
        val (sourceId, key) = SyncKeys.split(change.key).takeIf { it.size == 2 } ?: return false
        val dao = db.mangaDao()
        val local = dao.find(sourceId, key)
        if (change.deleted) {
            if (local?.inLibrary == true) dao.setInLibrary(local.id, false, null)
            return false
        }
        val record = decodeOrNull(MangaRecord.serializer(), change.payload) ?: return false
        val id = local?.id ?: dao.insert(placeholder(sourceId, key, record.title, now).copy(url = record.url, coverUrl = record.coverUrl))
        // A work first seen through one of its chapters gets its title and cover now.
        if (local?.updatedAt == 0L) fillPlaceholder(local, record, now)
        val wasIn = local?.inLibrary ?: false
        if (record.inLibrary != wasIn) dao.setInLibrary(id, record.inLibrary, if (record.inLibrary) record.addedAt ?: now else null)
        assignCategories(id, record.categories)
        record.reader?.let { db.readerSettingsDao().upsert(it.toEntity(id)) }
        if (record.shelf != local?.shelf) dao.setShelf(id, record.shelf)
        return record.inLibrary && !wasIn
    }

    /** The work's categories by name; a category not here yet is created after the others. */
    private suspend fun assignCategories(
        id: Long,
        names: List<String>,
    ) {
        val categoryDao = db.categoryDao()
        val categories = categoryDao.list().associateBy { categoryKey(it.name) }.toMutableMap()
        categoryDao.clearForManga(id)
        names.forEach { name ->
            val category =
                categories[categoryKey(name)] ?: run {
                    val order = (categories.values.maxOfOrNull { it.sortOrder } ?: 0) + 1
                    CategoryEntity(categoryDao.insert(CategoryEntity(name = name, sortOrder = order)), name, order).also {
                        categories[categoryKey(name)] =
                            it
                    }
                }
            categoryDao.assign(MangaCategoryEntity(id, category.id))
        }
    }

    private suspend fun fillPlaceholder(
        local: MangaEntity,
        record: MangaRecord,
        now: Long,
    ) = db.mangaDao().updateFromSource(
        local.id,
        record.url,
        record.title,
        local.alternativeTitles,
        record.coverUrl,
        local.authors,
        local.artists,
        local.status,
        local.genres,
        local.language,
        local.description,
        local.kind,
        now,
    )

    private suspend fun applyChapter(
        change: SyncChange,
        localPayload: String?,
        now: Long,
    ) {
        if (change.deleted) return
        val (sourceId, mangaKey, chapterKey) = SyncKeys.split(change.key).takeIf { it.size == CHAPTER_KEY_PARTS } ?: return
        val remote = decodeOrNull(ChapterRecord.serializer(), change.payload) ?: return
        val mangaDao = db.mangaDao()
        // A chapter can arrive before its work in a long first sync: the work comes in its own change.
        val mangaId = mangaDao.find(sourceId, mangaKey)?.id ?: mangaDao.insert(placeholder(sourceId, mangaKey, mangaKey, 0))
        val chapterDao = db.chapterDao()
        val chapterId =
            chapterDao.list(mangaId).firstOrNull { it.key == chapterKey }?.id ?: chapterDao.insert(
                ChapterEntity(
                    mangaId = mangaId,
                    key = chapterKey,
                    number = remote.number,
                    title = remote.title,
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
        val local = localPayload?.let { decodeOrNull(ChapterRecord.serializer(), it) }
        val merged = SyncRules.mergeChapter(local, remote)
        if (merged.read || merged.page > 0 || merged.progressAt > 0) {
            db.progressDao().upsert(ProgressEntity(chapterId, merged.page, merged.pageCount, merged.read, merged.progressAt))
        }
        val readAt = merged.readAt
        if (readAt != null &&
            readAt > (db.historyDao().get(chapterId)?.readAt ?: 0)
        ) {
            db.historyDao().upsert(HistoryEntity(chapterId, readAt))
        }
    }

    private suspend fun applySavedSearch(change: SyncChange) {
        val (sourceId, name) = SyncKeys.split(change.key).takeIf { it.size == 2 } ?: return
        val dao = db.searchDao()
        val existing = dao.listSaved().firstOrNull { it.sourceId == sourceId && it.name == name }
        if (change.deleted) {
            existing?.let { dao.deleteSaved(it.id) }
            return
        }
        val record = decodeOrNull(SavedSearchRecord.serializer(), change.payload) ?: return
        dao.upsertSaved(SavedSearchEntity(existing?.id ?: 0, sourceId, name, record.query, record.filters))
    }

    private suspend fun applyFeed(change: SyncChange) {
        val (sourceId, search) = SyncKeys.split(change.key).takeIf { it.size == 2 } ?: return
        val dao = db.searchDao()
        val searchId =
            if (search.isEmpty()) {
                null
            } else {
                dao.listSaved().firstOrNull { it.sourceId == sourceId && it.name == search }?.id
                    ?: return
            }
        val existing = dao.listFeed().firstOrNull { it.sourceId == sourceId && it.savedSearchId == searchId }
        if (change.deleted) {
            existing?.let { dao.deleteFeed(it.id) }
            return
        }
        val record = decodeOrNull(FeedRecord.serializer(), change.payload) ?: return
        if (existing == null) {
            dao.insertFeed(FeedItemEntity(sourceId = sourceId, savedSearchId = searchId, sortOrder = record.order))
        } else if (existing.sortOrder != record.order) {
            dao.setFeedOrder(existing.id, record.order)
        }
    }

    private suspend fun applyServer(change: SyncChange) {
        val dao = db.serverDao()
        if (change.deleted) {
            dao.delete(change.key)
            return
        }
        val record = decodeOrNull(ServerRecord.serializer(), change.payload) ?: return
        val pinned = dao.list().firstOrNull { it.sourceId == change.key }?.pinnedCertificate
        dao.upsert(ServerEntity(change.key, record.kind, record.name, record.url, record.username, pinned))
    }

    private fun applySetting(change: SyncChange) {
        if (change.key !in SETTINGS) return
        if (change.deleted) return
        val value = decodeOrNull(String.serializer(), change.payload) ?: return
        when (change.key) {
            AppPreferences.LANGUAGES -> prefs.setContentLanguages(value.split(',').filter { it.isNotBlank() }.toSet())
            AppPreferences.MANGADEX_SUGGESTIVE -> value.toBooleanStrictOrNull()?.let(prefs::setMangaDexSuggestive)
            AppPreferences.MANGADEX_LANGUAGES -> prefs.setMangaDexLanguages(value.split(',').filter { it.isNotBlank() }.toSet())
            AppPreferences.SOURCE_SHELVES -> prefs.setSourceShelves(Shelf.decode(value))
            else -> settings.putString(change.key, value)
        }
    }

    private fun readSetting(key: String): String? =
        when (key) {
            AppPreferences.MANGADEX_SUGGESTIVE -> settings.getBooleanOrNull(key)?.toString()
            else -> settings.getStringOrNull(key)
        }

    private fun placeholder(
        sourceId: String,
        key: String,
        title: String,
        now: Long,
    ) = MangaEntity(
        sourceId = sourceId,
        key = key,
        url = null,
        title = title,
        alternativeTitles = EMPTY_LIST,
        coverUrl = null,
        authors = EMPTY_LIST,
        artists = EMPTY_LIST,
        status = UNKNOWN,
        genres = EMPTY_LIST,
        language = null,
        description = null,
        kind = UNKNOWN,
        inLibrary = false,
        addedAt = null,
        coverColor = null,
        updatedAt = now,
    )

    private fun MangaReaderSettingsEntity.toRecord() = ReaderRecord(mode, scale, tapZones, background, doublePage, cropBorders)

    private fun ReaderRecord.toEntity(mangaId: Long) =
        MangaReaderSettingsEntity(mangaId, mode, scale, tapZones, background, doublePage, cropBorders)

    private fun <T> encode(
        serializer: KSerializer<T>,
        value: T,
    ): String = json.encodeToString(serializer, value)

    private fun <T> decodeOrNull(
        serializer: KSerializer<T>,
        text: String,
    ): T? = runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    companion object {
        private const val EMPTY_LIST = "[]"
        private const val CHAPTER_KEY_PARTS = 3
        private const val UNKNOWN = "UNKNOWN"
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            }

        /** The preferences that follow the account (RF-121); theme, Wi-Fi only and folders stay on the device. */
        val SETTINGS =
            listOf(
                "reader_settings",
                "library_options",
                AppPreferences.LANGUAGES,
                AppPreferences.MANGADEX_SUGGESTIVE,
                AppPreferences.MANGADEX_LANGUAGES,
                AppPreferences.SOURCE_SHELVES,
            )

        fun categoryKey(name: String): String = name.trim().lowercase()
    }
}
