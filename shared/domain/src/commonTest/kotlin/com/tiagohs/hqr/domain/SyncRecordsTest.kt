package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.repository.AccountState
import com.tiagohs.hqr.domain.repository.GoogleIdTokens
import com.tiagohs.hqr.domain.repository.SignInResult
import com.tiagohs.hqr.domain.sync.CategoryRecord
import com.tiagohs.hqr.domain.sync.ChapterRecord
import com.tiagohs.hqr.domain.sync.FeedRecord
import com.tiagohs.hqr.domain.sync.MangaRecord
import com.tiagohs.hqr.domain.sync.ReaderRecord
import com.tiagohs.hqr.domain.sync.SavedSearchRecord
import com.tiagohs.hqr.domain.sync.ServerRecord
import com.tiagohs.hqr.domain.sync.SyncChange
import com.tiagohs.hqr.domain.sync.SyncItemId
import com.tiagohs.hqr.domain.sync.SyncKind
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test

/** The account's items travel as JSON, the same on every platform (RF-121). */
class SyncRecordsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun RF121_records_round_trip_and_ignore_fields_of_newer_versions() {
        val manga =
            MangaRecord(
                title = "Frieren",
                url = "https://x",
                coverUrl = "https://c",
                inLibrary = true,
                addedAt = 5,
                categories = listOf("Lendo"),
                reader = ReaderRecord("WEBTOON", "FIT", "EDGES", "AUTO", doublePage = false, cropBorders = true),
            )
        json.decodeFromString(MangaRecord.serializer(), json.encodeToString(MangaRecord.serializer(), manga)) shouldBe manga
        val chapter = ChapterRecord(12.0, "Fim", read = true, page = 3, pageCount = 20, progressAt = 9, readAt = 9, unreadAt = null)
        json.decodeFromString(ChapterRecord.serializer(), json.encodeToString(ChapterRecord.serializer(), chapter)) shouldBe chapter
        json.decodeFromString(CategoryRecord.serializer(), """{"name":"Lendo","order":1,"color":"novo"}""") shouldBe
            CategoryRecord("Lendo", 1)
        json.decodeFromString(SavedSearchRecord.serializer(), """{"query":"a","filters":"{}"}""") shouldBe SavedSearchRecord("a", "{}")
        json.decodeFromString(FeedRecord.serializer(), """{"order":2}""") shouldBe FeedRecord(2)
        json.decodeFromString(ServerRecord.serializer(), """{"kind":"opds","name":"Casa","url":"https://k"}""") shouldBe
            ServerRecord("opds", "Casa", "https://k")
    }

    @Test
    fun RF119_account_shapes_and_the_default_pending_token() =
        runTest {
            SyncChange(SyncKind.MANGA, "k", "{}", 1, false).id shouldBe SyncItemId(SyncKind.MANGA, "k")
            AccountState.SignedIn("a@b.c").email shouldBe "a@b.c"
            (SignInResult.Done(3) as SignInResult) shouldBe SignInResult.Done(3)
            val tokens = GoogleIdTokens { "t" }
            tokens.request() shouldBe "t"
            tokens.pending() shouldBe null
        }
}
