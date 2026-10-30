package com.tiagohs.hqr.ui

import com.russhwolf.settings.PropertiesSettings
import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.presentation.reader.ReaderMode
import com.tiagohs.hqr.ui.di.DbReaderSettingsStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The reader's settings in the real store: suggestion, global and per-work (RF-37, RF-44, RF-46, RF-47). */
class ReaderSettingsStoreTest {
    private val db = inMemoryDatabase()
    private val store = DbReaderSettingsStore(db, AppPreferences(PropertiesSettings(java.util.Properties())))
    private val mangas = RoomMangaRepository(db, Clock.System)

    private fun work(
        key: String,
        kind: ReadingKind,
    ) = runBlocking { mangas.upsert(SourceManga(SourceId.MANGADEX, key, key, kind = kind)) }

    @Test
    fun RF37_aJapaneseMangaOpensRightToLeftEvenWithAGlobalMode() =
        runBlocking {
            store.saveGlobal(store.forManga(work("x", ReadingKind.UNKNOWN)).copy(mode = ReaderMode.LEFT_TO_RIGHT))
            assertEquals(ReaderMode.RIGHT_TO_LEFT, store.forManga(work("ja", ReadingKind.MANGA)).mode)
            assertEquals(ReaderMode.WEBTOON, store.forManga(work("ko", ReadingKind.MANHWA)).mode)
            // Unknown kind: the global mode.
            assertEquals(ReaderMode.LEFT_TO_RIGHT, store.forManga(work("u", ReadingKind.UNKNOWN)).mode)
        }

    @Test
    fun RF46_RF47_readerOptionsSavedForOneWorkStayForEveryWork() =
        runBlocking {
            val one = work("a", ReadingKind.MANGA)
            val settings =
                store.forManga(one).copy(
                    mode = ReaderMode.VERTICAL,
                    keepScreenOn = true,
                    volumeKeys = true,
                    splitWide = true,
                    invertPages = true,
                    showPageNumber = false,
                )
            store.saveForManga(one.id, settings)
            val again = store.forManga(one)
            assertEquals(ReaderMode.VERTICAL, again.mode)
            val other = store.forManga(work("b", ReadingKind.MANGA))
            // The work's mode stays with it; the reader's options reach the other work.
            assertEquals(ReaderMode.RIGHT_TO_LEFT, other.mode)
            assertTrue(other.keepScreenOn && other.volumeKeys && other.splitWide && other.invertPages && !other.showPageNumber)
        }
}
