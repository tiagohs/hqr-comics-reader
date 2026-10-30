package com.tiagohs.hqr.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.tiagohs.hqr.data.db.HqrDatabase
import com.tiagohs.hqr.data.db.buildHqr
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.domain.model.SourceId
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.time.Clock

/**
 * Each schema step keeps the user's data (ADR-004, R-09): a library written with the exported schema of version 1
 * opens with the current version.
 */
class MigrationTest {
    /** Builds the file exactly as version [version] left it, from its exported schema. */
    private fun createAt(
        version: Int,
        file: File,
        rows: List<String>,
    ) {
        val schema = Json.parseToJsonElement(File("schemas/com.tiagohs.hqr.data.db.HqrDatabase/$version.json").readText())
        val db = schema.jsonObject["database"]!!.jsonObject
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        db["entities"]!!.jsonArray.forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val sqls =
                listOf(entity.jsonObject["createSql"]!!.jsonPrimitive.content) +
                    (entity.jsonObject["indices"]?.jsonArray?.map { it.jsonObject["createSql"]!!.jsonPrimitive.content } ?: emptyList())
            sqls.forEach { connection.execSQL(it.replace("\${TABLE_NAME}", table)) }
        }
        db["setupQueries"]!!.jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
        rows.forEach { connection.execSQL(it) }
        connection.execSQL("PRAGMA user_version = $version")
        connection.close()
    }

    @Test
    fun RF89_libraryFromVersion1OpensWithTheCurrentVersion() =
        runTest {
            val file = File.createTempFile("library-v1", ".db").also { it.delete() }
            createAt(
                1,
                file,
                listOf(
                    "INSERT INTO manga (id, sourceId, `key`, url, title, alternativeTitles, coverUrl, authors, artists, status, genres, " +
                        "language, description, kind, inLibrary, addedAt, coverColor, updatedAt) VALUES (1, 'mangadex', 'abc', NULL, " +
                        "'Obra', '[]', NULL, '[]', '[]', 'ONGOING', '[]', 'pt-br', NULL, 'MANGA', 1, 10, NULL, 10)",
                ),
            )
            val db = Room.databaseBuilder<HqrDatabase>(file.absolutePath).setDriver(BundledSQLiteDriver()).buildHqr()
            val work = RoomMangaRepository(db, Clock.System).find(SourceId.MANGADEX, "abc")!!
            work.title shouldBe "Obra"
            work.inLibrary shouldBe true
            db.close()
            file.delete()
        }
}
