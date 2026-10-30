package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.ui.navigation.HqrApp
import kotlin.test.AfterTest
import kotlin.test.Test

/** Phase 3 through the app: global search with one source down (RF-23, RF-05, RF-28). */
@OptIn(ExperimentalTestApi::class)
class SearchScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    /** A source that is down: its group shows the error, the others show results. */
    private class DownSource : SearchSource {
        override val id = SourceId("down")
        override val name = "HQ Now"
        override val languages = listOf("pt-br")
        var down = true

        override fun webUrl(query: String) = "https://example.org/busca"

        override suspend fun search(
            query: String,
            filters: List<SourceFilter>,
            page: Int,
        ): MangaPage {
            if (down) throw SourceException("fora do ar", SourceException.Kind.SERVER)
            if (query == "xyzq") return MangaPage(emptyList(), false)
            return MangaPage(listOf(SourceManga(id, "x", "Turma do Bairro")), false)
        }

        override suspend fun details(manga: SourceManga) = manga

        override suspend fun chapters(manga: SourceManga) = emptyList<com.tiagohs.hqr.domain.model.SourceChapter>()

        override suspend fun pages(
            manga: SourceManga,
            chapter: com.tiagohs.hqr.domain.model.SourceChapter,
        ) = emptyList<com.tiagohs.hqr.domain.model.SourcePage>()
    }

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 5_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    @Test
    fun RF23_RF05_globalSearchKeepsTheBrokenSourceApart() =
        screenTest {
            val down = DownSource()
            startTestKoin { listOf(DemoSource(), down) }
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Search every source"))
            screenshot("demo-SCR-EXP-001-search")
            onAllNodes(hasText("Search every source"))[0].performClick()
            waitFor(hasSetTextAction())
            onAllNodes(hasSetTextAction())[0].performTextInput("kaiju")
            // The keyboard's search key skips the 400 ms pause.
            onAllNodes(hasSetTextAction())[0].performImeAction()
            waitFor(hasText("Kaiju Express"))
            waitFor(hasText("Try again"))
            // RF-05: the failed source also offers its web page.
            waitFor(hasText("Open in browser"))
            screenshot("demo-SCR-SRCH-001")
            down.down = false
            onAllNodes(hasText("Try again"))[0].performClick()
            waitFor(hasText("Turma do Bairro"))
            // "More from" opens the source already on this search.
            onAllNodes(hasText("More from MangaDex"))[0].performClick()
            waitFor(hasText("kaiju"))
            waitFor(hasText("Kaiju Express"))
        }

    @Test
    fun RF22_filtersSheetIncludesExcludesAndSearches() =
        screenTest {
            val demo = DemoSource()
            startTestKoin { listOf(demo) }
            setAppContent { HqrApp(systemDark = true) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            waitFor(hasText("Filters"))
            onAllNodes(hasText("Filters"))[0].performClick()
            waitFor(hasText("Romance"))
            onAllNodes(hasText("Romance"))[0].performClick()
            onAllNodes(hasText("Horror"))[0].performScrollTo().performClick()
            onAllNodes(hasText("Horror"))[0].performScrollTo().performClick()
            waitFor(hasText("Without Horror"))
            onAllNodes(hasText("Completed"))[0].performScrollTo().performClick()
            screenshot("demo-SCR-EXP-003")
            onAllNodes(hasText("Show results"))[0].performClick()
            waitFor(hasText("Filters · 2")) // tags and status: one per group
            val tags = demo.lastFilters.filterIsInstance<SourceFilter.Tags>().single()
            kotlin.test.assertEquals(setOf("romance"), tags.included)
            kotlin.test.assertEquals(setOf("horror"), tags.excluded)
        }

    @Test
    fun RF28_nothingFoundShowsTheTermAndTheWayToTheSources() =
        screenTest {
            startTestKoin { listOf(DownSource().apply { down = false }) }
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Search every source"))
            onAllNodes(hasText("Search every source"))[0].performClick()
            waitFor(hasSetTextAction())
            onAllNodes(hasSetTextAction())[0].performTextInput("xyzq")
            onAllNodes(hasSetTextAction())[0].performImeAction()
            waitFor(hasText("Nothing found for “xyzq”"))
            screenshot("demo-SCR-SRCH-001-nothing")
            onAllNodes(hasText("See sources"))[0].performClick()
            waitFor(hasText("Sources"))
        }
}
