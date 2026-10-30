package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomCategoryRepository
import com.tiagohs.hqr.data.repository.RoomChapterRepository
import com.tiagohs.hqr.data.repository.RoomHistoryRepository
import com.tiagohs.hqr.data.repository.RoomMangaRepository
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import kotlin.time.Clock
import kotlin.time.Instant

val fixedClock =
    object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(1_790_000_000_000)
    }

/** The app's repositories over an in-memory SQLite. */
class TestDb(
    clock: Clock = fixedClock,
) {
    val db = inMemoryDatabase()
    val mangas = RoomMangaRepository(db, clock)
    val chapters = RoomChapterRepository(db, clock)
    val categories = RoomCategoryRepository(db)
    val history = RoomHistoryRepository(db, mangas, chapters)
}

/** A scripted source: pages of 2 works, a known chapter list, and a switch to fail. */
class FakeSource(
    override val id: SourceId = SourceId("fake"),
    private val pages: Int = 3,
) : CatalogSource,
    SearchSource {
    override val name = "Fake"
    override val languages = listOf("pt-br")
    var fail: SourceException.Kind? = null
    var detailsCover: String? = null
    var chapterList = listOf(SourceChapter("c1", 1.0), SourceChapter("c2", 2.0), SourceChapter("c3", 3.0))
    val requested = mutableListOf<String>()

    private fun page(
        prefix: String,
        page: Int,
    ): MangaPage {
        fail?.let { throw SourceException("falhou", it) }
        requested += "$prefix$page"
        return MangaPage((1..2).map { SourceManga(id, "$prefix$page-$it", "Obra $prefix$page-$it") }, page < pages)
    }

    override suspend fun popular(page: Int) = page("p", page)

    override suspend fun latest(page: Int) = page("l", page)

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ) = page("s:$query:", page)

    override suspend fun details(manga: SourceManga): SourceManga {
        fail?.let { throw SourceException("falhou", it) }
        return manga.copy(description = "Sinopse de ${manga.title}", authors = listOf("Autora"), coverUrl = detailsCover ?: manga.coverUrl)
    }

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        fail?.let { throw SourceException("falhou", it) }
        return chapterList
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ) = (0 until 22).map { SourcePage(it, "https://img/${chapter.key}/$it.jpg") }
}
