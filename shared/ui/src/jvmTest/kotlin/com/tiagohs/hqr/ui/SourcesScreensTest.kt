package com.tiagohs.hqr.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import com.tiagohs.hqr.data.settings.AppPreferences
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.ui.navigation.HqrApp
import io.kotest.matchers.shouldBe
import org.koin.core.context.GlobalContext
import kotlin.test.AfterTest
import kotlin.test.Test

/** Phase 4 through the app: the sources screen, its switches and the server form. */
@OptIn(ExperimentalTestApi::class)
class SourcesScreensTest {
    @AfterTest
    fun tearDown() = stopTestKoin()

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) {
        runCatching { waitUntil(timeoutMillis = 5_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure {
                onRoot().printToLog("TREE")
                throw it
            }
    }

    private fun ComposeUiTest.gone(matcher: SemanticsMatcher) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }

    @Test
    fun RF03_RF17_RF95_switchHidesTheSourceAndTheFormSaysTheCause() =
        screenTest {
            startTestKoin()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Manage sources and servers"))
            onAllNodes(hasText("Manage sources and servers"))[0].performScrollTo().performClick()
            waitFor(hasText("Online sources"))
            screenshot("demo-SCR-SRC-001")
            // RF-03: turned off, the source leaves Explore.
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("Manage sources and servers"))
            gone(hasText("MangaDex"))
            onAllNodes(hasText("Manage sources and servers"))[0].performClick()
            waitFor(hasText("Online sources"))
            waitFor(hasText("MangaDex"))
            onAllNodes(hasText("MangaDex"))[0].performClick()
            // RF-17 and RF-95: the form refuses a login written in the address, before any request.
            onAllNodes(hasText("Add server"))[0].performScrollTo().performClick()
            waitFor(hasSetTextAction())
            onAllNodes(hasSetTextAction())[1].performTextInput("https://tiago:x@komga.casa/opds")
            onAllNodes(hasText("Test"))[0].performClick()
            waitFor(hasText("Put the user and password in their own fields, not in the address."))
            screenshot("demo-SCR-SRV-001")
        }

    @Test
    fun RF06_RF08_mangaDexSettingsChooseLanguagesQualityAndSuggestive() =
        screenTest {
            startTestKoin(sources = { listOf(DemoSource(id = SourceId.MANGADEX)) })
            val prefs = GlobalContext.get().get<AppPreferences>()
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Manage sources and servers"))
            onAllNodes(hasText("Manage sources and servers"))[0].performScrollTo().performClick()
            waitFor(hasContentDescription("MangaDex settings"))
            onAllNodes(hasContentDescription("MangaDex settings"))[0].performClick()
            waitFor(hasText("Chapter languages"))
            // RF-06: only this source asks for English too; the content languages stay as they were.
            onAllNodes(hasText("English"))[0].performClick()
            prefs.mangaDexLanguages() shouldBe setOf("pt-br", "en")
            prefs.contentLanguages.value shouldBe setOf("pt-br")
            onAllNodes(hasText("Data saver"))[0].performClick()
            prefs.mangaDexDataSaver.value shouldBe true
            screenshot("demo-SCR-SRC-002")
            // Back to following the content languages.
            onAllNodes(hasText("Same as the content languages"))[0].performClick()
            prefs.mangaDexLanguagesOverride.value shouldBe null
            // RF-08: suggestive only after the 18+ confirmation.
            onAllNodes(hasText("Suggestive content"))[0].performScrollTo().performClick()
            waitFor(hasText("I am 18 or older"))
            prefs.mangaDexSuggestive.value shouldBe false
            onAllNodes(hasText("I am 18 or older"))[0].performClick()
            prefs.mangaDexSuggestive.value shouldBe true
        }

    /** A source behind Cloudflare's check (RF-106). */
    private class Guarded : com.tiagohs.hqr.domain.source.CatalogSource {
        override val id = SourceId("guarded")
        override val name = "Guarded"
        override val languages = listOf("en")

        override fun webUrl(query: String) = "https://guarded.example"

        private fun fail(): Nothing =
            throw com.tiagohs.hqr.domain.source
                .SourceException("x", com.tiagohs.hqr.domain.source.SourceException.Kind.CHALLENGE)

        override suspend fun popular(page: Int) = fail()

        override suspend fun latest(page: Int) = fail()

        override suspend fun details(manga: com.tiagohs.hqr.domain.model.SourceManga) = fail()

        override suspend fun chapters(manga: com.tiagohs.hqr.domain.model.SourceManga) = fail()

        override suspend fun pages(
            manga: com.tiagohs.hqr.domain.model.SourceManga,
            chapter: com.tiagohs.hqr.domain.model.SourceChapter,
        ) = fail()
    }

    @Test
    fun RF106_aSourceAskingForVerificationOffersItsPage() =
        screenTest {
            startTestKoin(sources = { listOf(Guarded()) })
            setAppContent { HqrApp(systemDark = false) }
            waitFor(hasText("Explore"))
            onAllNodes(hasText("Explore"))[0].performClick()
            waitFor(hasText("Guarded"))
            onAllNodes(hasText("Guarded"))[0].performClick()
            waitFor(hasText("The source asks for a verification"))
            screenshot("demo-SCR-STA-001-challenge")
            onAllNodes(hasText("Verify"))[0].performClick()
            // On the desktop tests there is no browser inside the app: the screen says where it opens.
            waitFor(hasText("The verification opens only in the phone app. Open the source's site in your browser and try again."))
            onAllNodes(hasContentDescription("Back"))[0].performClick()
            waitFor(hasText("The source asks for a verification"))
        }
}
