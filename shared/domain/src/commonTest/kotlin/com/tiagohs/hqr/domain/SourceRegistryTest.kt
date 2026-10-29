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
