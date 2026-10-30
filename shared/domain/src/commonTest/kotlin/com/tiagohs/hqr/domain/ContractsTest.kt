package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.Category
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.repository.DeletedCategory
import com.tiagohs.hqr.domain.repository.DownloadItem
import com.tiagohs.hqr.domain.repository.DownloadState
import com.tiagohs.hqr.domain.repository.LibraryRemoval
import com.tiagohs.hqr.domain.repository.RestoreFailure
import com.tiagohs.hqr.domain.repository.RestoreProblem
import com.tiagohs.hqr.domain.repository.RestoreSummary
import com.tiagohs.hqr.domain.source.CatalogEntry
import com.tiagohs.hqr.domain.source.CatalogPage
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.ServerInput
import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.domain.source.SourceFilter
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** The shapes the other layers share: defaults, equality and the default methods of the contracts. */
class ContractsTest {
    private val search =
        object : SearchSource {
            override val id = SourceId("s")
            override val name = "S"
            override val languages = emptyList<String>()

            override suspend fun search(
                query: String,
                filters: List<SourceFilter>,
                page: Int,
            ) = MangaPage(emptyList(), hasNextPage = false)

            override suspend fun details(manga: SourceManga) = manga

            override suspend fun chapters(manga: SourceManga) = emptyList<SourceChapter>()

            override suspend fun pages(
                manga: SourceManga,
                chapter: SourceChapter,
            ) = emptyList<SourcePage>()
        }

    @Test
    fun RF22_RF05_sourceDefaults() =
        runTest {
            search.filters() shouldBe emptyList()
            search.webUrl("x") shouldBe null
            search.schemaVersion shouldBe 1
            search.search("", emptyList(), 1) shouldBe MangaPage(emptyList(), false)
        }

    @Test
    fun RF17_RF18_RF19_serverShapes() {
        Server(SourceId("opds-1"), "Casa", "https://k", null).copy(username = "t").username shouldBe "t"
        ServerInput("Casa", "https://k").password shouldBe ""
        ServerProblem.entries.size shouldBe 7
        CatalogPage("Raiz", listOf(CatalogEntry("Séries", "u", isBook = false))).canSearch shouldBe false
        CatalogEntry("Livro", "u", isBook = true, coverUrl = "c", subtitle = "a").coverUrl shouldBe "c"
    }

    @Test
    fun RF52_RF53_RF62_RF73_dataShapes() {
        LibraryRemoval(1, 10, setOf(2)).categoryIds shouldBe setOf(2L)
        DeletedCategory(Category(1, "A", 1), setOf(3)).mangaIds shouldBe setOf(3L)
        DownloadItem(1, 2, "Obra", "1", DownloadState.QUEUED, 0, 10).copy(state = DownloadState.DONE).state shouldBe DownloadState.DONE
        DownloadState.valueOf("WAITING_WIFI") shouldBe DownloadState.WAITING_WIFI
        RestoreSummary(1, 2, 3, 4, listOf("HQ Now")).unavailableSources shouldBe listOf("HQ Now")
        RestoreFailure(RestoreProblem.NOT_A_BACKUP).problem shouldBe RestoreProblem.NOT_A_BACKUP
        RestoreProblem.entries.size shouldBe 2
    }
}
