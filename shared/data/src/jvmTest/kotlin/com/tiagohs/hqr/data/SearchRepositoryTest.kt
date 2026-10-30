package com.tiagohs.hqr.data

import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.data.repository.RoomSearchRepository
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceFilter
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/** Saved searches, the feed and recent searches over a real database (RF-24, RF-25, RF-27). */
class SearchRepositoryTest {
    private val db = inMemoryDatabase()
    private var now = 1_000L
    private val repo =
        RoomSearchRepository(
            db,
            object : Clock {
                override fun now() = Instant.fromEpochMilliseconds(now++)
            },
        )
    private val romance = listOf(SourceFilter.Tags("tags", listOf(SourceFilter.Option("r", "Romance")), included = setOf("r")))

    @AfterTest
    fun close() = db.close()

    @Test
    fun RF24_saveRenameAndDeleteKeepTextAndFilters() =
        runTest {
            val id = repo.save(SourceId.MANGADEX, " Romance concluído ", "amor", romance)
            repo.save(SourceId.HQ_NOW, "Marvel", "", emptyList())
            val saved = repo.observeSaved(SourceId.MANGADEX).first().single()
            saved.name shouldBe "Romance concluído"
            saved.query shouldBe "amor"
            saved.filters shouldBe romance
            repo.rename(id, "Romances")
            repo
                .observeSaved(SourceId.MANGADEX)
                .first()
                .single()
                .name shouldBe "Romances"
            repo.addToFeed(SourceId.MANGADEX, id) shouldBe true
            // Deleting it also takes it out of the feed.
            repo.delete(id)
            repo.observeSaved(SourceId.MANGADEX).first() shouldBe emptyList()
            repo.observeFeed().first() shouldBe emptyList()
            repo.observeAllSaved().first().map { it.name } shouldContainExactly listOf("Marvel")
        }

    @Test
    fun RF25_feedTakesFiveItemsWithoutRepeats() =
        runTest {
            val id = repo.save(SourceId.MANGADEX, "Romance", "", romance)
            repo.addToFeed(SourceId.MANGADEX, null) shouldBe true
            repo.addToFeed(SourceId.MANGADEX, null) shouldBe false
            repo.addToFeed(SourceId.MANGADEX, id) shouldBe true
            repo.addToFeed(SourceId.HQ_NOW, null) shouldBe true
            repo.addToFeed(SourceId("a"), null) shouldBe true
            repo.addToFeed(SourceId("b"), null) shouldBe true
            repo.addToFeed(SourceId("c"), null) shouldBe false
            val feed = repo.observeFeed().first()
            feed.size shouldBe 5
            feed[1].savedSearch?.name shouldBe "Romance"
            repo.removeFromFeed(feed[0].id)
            repo.observeFeed().first().size shouldBe 4
        }

    @Test
    fun RF27_tenNewestRecentSearchesAndRemove() =
        runTest {
            (1..12).forEach { repo.addRecent("busca $it") }
            repo.addRecent("  ")
            // The same text again moves up instead of repeating.
            repo.addRecent("busca 3")
            val recent = repo.observeRecent().first()
            recent.size shouldBe 10
            recent.first() shouldBe "busca 3"
            recent shouldContainExactly listOf("busca 3") + (12 downTo 4).filter { it != 3 }.map { "busca $it" }.take(9)
            repo.removeRecent("busca 3")
            repo.observeRecent().first().first() shouldBe "busca 12"
        }
}
