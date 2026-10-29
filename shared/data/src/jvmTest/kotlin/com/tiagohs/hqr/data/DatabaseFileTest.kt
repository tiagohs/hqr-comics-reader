package com.tiagohs.hqr.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.buildHqr
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.time.Clock

/**
 * R-09: the database file opens again with what was saved, and the schema exported for this version exists, so the
 * next version can declare its migration from it (each migration gets its own test then).
 */
class DatabaseFileTest {
    private fun open(file: File): HqrDatabase =
        Room.databaseBuilder<HqrDatabase>(file.absolutePath).setDriver(BundledSQLiteDriver()).buildHqr()

    @Test
    fun RF89_savedWorksSurviveReopeningTheFile() =
        runTest {
            val file = File.createTempFile("library", ".db").also { it.delete() }
            val first = open(file)
            RoomMangaRepository(first, Clock.System).upsert(SourceManga(SourceId.MANGADEX, "abc", "Obra"))
            first.close()
            val again = open(file)
            RoomMangaRepository(again, Clock.System).find(SourceId.MANGADEX, "abc")?.title shouldBe "Obra"
            again.close()
            file.delete()
        }

    @Test
    fun RF89_schemaOfThisVersionIsExported() {
        val schema = File("schemas/com.tiagohs.hqr.data.db.HqrDatabase/${HqrDatabase.VERSION}.json")
        schema.exists() shouldBe true
    }
}
