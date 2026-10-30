package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.model.chapterLabel
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlin.test.Test

class SourceRegistryTest {
    private class Stub(
        override val id: SourceId,
    ) : CatalogSource {
        override val name = id.value
        override val languages = emptyList<String>()

        override suspend fun popular(page: Int) = MangaPage(emptyList(), false)

        override suspend fun latest(page: Int) = MangaPage(emptyList(), false)

        override suspend fun details(manga: SourceManga) = manga

        override suspend fun chapters(manga: SourceManga) = emptyList<SourceChapter>()

        override suspend fun pages(
            manga: SourceManga,
            chapter: SourceChapter,
        ) = emptyList<SourcePage>()
    }

    @Test
    fun RF02_builtInSourcesInOrderAndServersAtRuntime() {
        val registry = SourceRegistry(listOf(Stub(SourceId.MANGADEX), Stub(SourceId.LOCAL)))
        registry.all.map { it.id } shouldBe listOf(SourceId.MANGADEX, SourceId.LOCAL)
        val server = Stub(SourceId("opds-1"))
        registry.register(server)
        registry[SourceId("opds-1")] shouldBe server
        registry.unregister(server.id)
        registry[server.id] shouldBe null
    }

    @Test
    fun RF03_RF105_userAndListSwitches() {
        val registry = SourceRegistry(listOf(Stub(SourceId.MANGADEX), Stub(SourceId.HQ_NOW), Stub(SourceId.LOCAL)))
        registry.setUserDisabled(setOf(SourceId.HQ_NOW))
        registry.active.value.map { it.id } shouldBe listOf(SourceId.MANGADEX, SourceId.LOCAL)
        // Turned off by the user: out of Explore and search, but its works still open.
        registry[SourceId.HQ_NOW] shouldBe registry.everything[1]
        registry.status(SourceId.HQ_NOW) shouldBe SourceRegistry.Status.OFF_BY_USER
        registry.setListDisabled(setOf(SourceId.MANGADEX))
        // Turned off by the remote list: unavailable everywhere.
        shouldThrow<SourceException> { registry.require(SourceId.MANGADEX) }.kind shouldBe SourceException.Kind.UNAVAILABLE
        registry.active.value.map { it.id } shouldBe listOf(SourceId.LOCAL)
        registry.all.map { it.id } shouldBe listOf(SourceId.HQ_NOW, SourceId.LOCAL)
        registry.status(SourceId.MANGADEX) shouldBe SourceRegistry.Status.OFF_BY_LIST
    }

    @Test
    fun RF88_lookupsWaitUntilServersAreRegistered() =
        kotlinx.coroutines.test.runTest {
            val registry = SourceRegistry(listOf(Stub(SourceId.MANGADEX)), ready = false)
            val server = Stub(SourceId("opds-1"))
            val found =
                async {
                    registry.awaitRequire(server.id)
                }
            kotlinx.coroutines.yield()
            registry.register(server)
            registry.markReady()
            found.await() shouldBe server
            registry.version.value shouldBe 2
        }

    @Test
    fun RF22_filtersKnowWhenTheyAreAtTheirDefaults() {
        val option =
            com.tiagohs.hqr.domain.source.SourceFilter
                .Option("a", "A")
        com.tiagohs.hqr.domain.source.SourceFilter
            .Tags("t", listOf(option))
            .isDefault shouldBe true
        com.tiagohs.hqr.domain.source.SourceFilter
            .Tags("t", listOf(option), excluded = setOf("a"))
            .isDefault shouldBe false
        com.tiagohs.hqr.domain.source.SourceFilter
            .Choice("c", listOf(option), setOf("a"))
            .isDefault shouldBe false
        com.tiagohs.hqr.domain.source.SourceFilter
            .Text("y", " ")
            .isDefault shouldBe true
        com.tiagohs.hqr.domain.source.SourceFilter
            .Sort("o", listOf(option), "a")
            .isDefault shouldBe true
        Stub(SourceId.MANGADEX).webUrl() shouldBe null
    }

    @Test
    fun RF90_missingSourceIsUnavailable() {
        val registry = SourceRegistry(emptyList())
        shouldThrow<SourceException> { registry.require(SourceId.HQ_NOW) }.kind shouldBe SourceException.Kind.UNAVAILABLE
        SourceId.HQ_NOW.toString() shouldBe "hqnow"
    }

    @Test
    fun RF30_chapterLabels() {
        12.0.chapterLabel() shouldBe "12"
        10.5.chapterLabel() shouldBe "10.5"
    }
}
