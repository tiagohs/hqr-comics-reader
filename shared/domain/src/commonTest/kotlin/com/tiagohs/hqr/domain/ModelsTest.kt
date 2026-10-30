package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.Chapter
import com.tiagohs.hqr.domain.model.Manga
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.repository.HistoryEntry
import com.tiagohs.hqr.domain.repository.LibraryItem
import com.tiagohs.hqr.domain.source.SourceFilter
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ModelsTest {
    private val manga =
        SourceManga(
            sourceId = SourceId.MANGADEX,
            key = "abc",
            title = "Obra",
            alternativeTitles = listOf("Work"),
            coverUrl = "https://c/1.jpg",
            url = "https://site/abc",
            authors = listOf("A"),
            artists = listOf("B"),
            status = PublicationStatus.COMPLETED,
            genres = listOf("Ação"),
            description = "Sinopse",
            language = "pt-br",
            kind = ReadingKind.MANHWA,
        )

    @Test
    fun RF24_filtersSurviveJson() {
        val options = listOf(SourceFilter.Option("action", "Ação"), SourceFilter.Option("drama", "Drama"))
        val filters: List<SourceFilter> =
            listOf(
                SourceFilter.Tags("tags", options, included = setOf("action"), excluded = setOf("drama")),
                SourceFilter.Choice("status", options, selected = setOf("action"), multiple = false),
                SourceFilter.Sort("order", options, selected = "drama", ascending = true),
            )
        val json = Json.encodeToString(filters)
        Json.decodeFromString<List<SourceFilter>>(json) shouldBe filters
        filters.map { it.key } shouldBe listOf("tags", "status", "order")
    }

    @Test
    fun RF85_sourceModelsSurviveJson() {
        Json.decodeFromString<SourceManga>(Json.encodeToString(manga)) shouldBe manga
        val chapter =
            SourceChapter("c1", 1.5, title = "Um", group = "G", uploadedAtEpochMs = 10, externalUrl = "https://site/c1", volume = "2")
        Json.decodeFromString<SourceChapter>(Json.encodeToString(chapter)) shouldBe chapter
        val pages = listOf(SourcePage(0, "https://i/0.jpg", mapOf("Referer" to "https://site/")))
        Json.decodeFromString<List<SourcePage>>(Json.encodeToString(pages)) shouldBe pages
        Json.decodeFromString<SourceId>(Json.encodeToString(SourceId.LOCAL)) shouldBe SourceId.LOCAL
    }

    @Test
    fun RF26_libraryModelsExposeTheirSource() {
        val work = Manga(id = 7, source = manga, inLibrary = true, addedAtEpochMs = 1, coverColor = 0x112233)
        work.sourceId shouldBe SourceId.MANGADEX
        work.key shouldBe "abc"
        work.title shouldBe "Obra"
        val chapter = Chapter(1, 7, SourceChapter("c1", 1.0), read = false, lastPage = 3, pageCount = 10)
        chapter.inProgress shouldBe true
        val item =
            LibraryItem(
                work,
                unread = 2,
                downloaded = 0,
                categoryIds = setOf(0),
                lastReadAtEpochMs = null,
                latestChapterAtEpochMs = 5,
                totalChapters = 4,
            )
        item.copy(unread = 0).unread shouldBe 0
        HistoryEntry(work, chapter, 9).readAtEpochMs shouldBe 9
        Category(Category.DEFAULT_ID, "Padrão", 0).isDefault shouldBe true
        Category(3, "Lendo", 1).isDefault shouldBe false
    }
}
