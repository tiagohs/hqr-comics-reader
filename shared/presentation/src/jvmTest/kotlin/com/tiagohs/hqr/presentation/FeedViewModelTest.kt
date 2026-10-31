package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.repository.FeedItem
import com.tiagohs.hqr.domain.repository.SavedSearch
import com.tiagohs.hqr.domain.repository.SearchRepository
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.common.ErrorKind
import com.tiagohs.hqr.presentation.feed.FeedViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** The feed: items from several origins merged by date (RF-25). */
class FeedViewModelTest : MainDispatcherTest() {
    private class Fake(
        override val id: SourceId,
        var latest: List<SourceManga> = emptyList(),
        private val found: List<SourceManga> = emptyList(),
        var fails: Boolean = false,
    ) : CatalogSource,
        SearchSource {
        override val name = id.value.uppercase()
        override val languages = listOf("pt-br")
        var searched: Pair<String, List<SourceFilter>>? = null

        override suspend fun popular(page: Int) = MangaPage(emptyList(), false)

        var asked = 0

        override suspend fun latest(page: Int): MangaPage {
            asked++
            if (fails) throw SourceException("fora", SourceException.Kind.SERVER)
            return MangaPage(latest, false)
        }

        override suspend fun search(
            query: String,
            filters: List<SourceFilter>,
            page: Int,
        ): MangaPage {
            searched = query to filters
            return MangaPage(found, false)
        }

        override suspend fun details(manga: SourceManga) = manga

        override suspend fun chapters(manga: SourceManga) = emptyList<com.tiagohs.hqr.domain.model.SourceChapter>()

        override suspend fun pages(
            manga: SourceManga,
            chapter: com.tiagohs.hqr.domain.model.SourceChapter,
        ) = emptyList<com.tiagohs.hqr.domain.model.SourcePage>()
    }

    private fun manga(
        source: String,
        key: String,
        at: Long?,
    ) = SourceManga(SourceId(source), key, key, updatedAtEpochMs = at)

    private class Feed(
        items: List<FeedItem>,
    ) : SearchRepository {
        val rows = MutableStateFlow(items)

        override fun observeSaved(sourceId: SourceId) = flowOf(emptyList<SavedSearch>())

        override fun observeAllSaved() = flowOf(emptyList<SavedSearch>())

        override suspend fun save(
            sourceId: SourceId,
            name: String,
            query: String,
            filters: List<SourceFilter>,
        ) = 0L

        override suspend fun rename(
            id: Long,
            name: String,
        ) = Unit

        override suspend fun delete(id: Long) = Unit

        override fun observeFeed() = rows

        override suspend fun addToFeed(
            sourceId: SourceId,
            savedSearchId: Long?,
        ): Boolean {
            val present = rows.value.any { it.sourceId == sourceId && it.savedSearch?.id == savedSearchId }
            if (present || rows.value.size >= SearchRepository.MAX_FEED) return false
            rows.value = rows.value + FeedItem((rows.value.maxOfOrNull { it.id } ?: 0) + 1, sourceId, null)
            return true
        }

        override suspend fun removeFromFeed(id: Long) {
            rows.value = rows.value.filter { it.id != id }
        }

        override fun observeRecent() = flowOf(emptyList<String>())

        override suspend fun addRecent(query: String) = Unit

        override suspend fun removeRecent(query: String) = Unit
    }

    @Test
    fun RF25_newestFirstFromEveryItemUndatedLastAndAFailureApart() =
        runTest {
            val romance = SavedSearch(7, SourceId("hq"), "Romance concluído", "amor", emptyList())
            val md = Fake(SourceId("md"), latest = listOf(manga("md", "a", 300), manga("md", "b", null)))
            val hq = Fake(SourceId("hq"), found = listOf(manga("hq", "c", 500), manga("hq", "d", 100)))
            val down = Fake(SourceId("down"), fails = true)
            val repo = Feed(listOf(FeedItem(1, md.id, null), FeedItem(2, hq.id, romance), FeedItem(3, down.id, null)))
            val vm = track(FeedViewModel(repo, SourceRegistry(listOf(md, hq, down)), TestDb().mangas))
            val state = vm.state.first { !it.loading }
            state.entries.map { it.manga.key } shouldContainExactly listOf("c", "a", "d", "b")
            state.entries.map { it.origin } shouldContainExactly listOf("Romance concluído", "MD", "Romance concluído", "MD")
            hq.searched shouldBe ("amor" to emptyList())
            state.failed shouldBe mapOf("DOWN" to ErrorKind.SERVER)
            vm.remove(repo.rows.value.last())
            // Taken out, the failing item is asked no more.
            vm.state
                .first { !it.loading && it.failed.isEmpty() }
                .entries.size shouldBe 4
        }

    @Test
    fun RF24_savedSearchAppliesTextAndFiltersAndListIsMadeAgain() =
        runTest {
            val db = TestDb()
            val repo =
                com.tiagohs.hqr.data.repository
                    .RoomSearchRepository(db.db)
            val md = Fake(SourceId("md"), found = listOf(manga("md", "x", 1)))
            val tags = listOf(SourceFilter.Tags("tags", listOf(SourceFilter.Option("r", "Romance")), included = setOf("r")))
            val vm =
                track(
                    com.tiagohs.hqr.presentation.browse.BrowseSourceViewModel(
                        md.id,
                        SourceRegistry(listOf(md)),
                        db.mangas,
                        searches = repo,
                    ),
                )
            vm.search("amor", tags)
            vm.saveSearch("Romance concluído")
            val saved = vm.savedSearches.first { it.isNotEmpty() }.single()
            vm.search("outra coisa", emptyList())
            vm.applySaved(saved)
            val state = vm.state.first { it.query == "amor" && !it.loading }
            state.filters shouldBe tags
            md.searched shouldBe ("amor" to tags)
            vm.addToFeed(saved) shouldBe true
            repo
                .observeFeed()
                .first()
                .single()
                .savedSearch
                ?.name shouldBe "Romance concluído"
        }

    @Test
    fun RF27_aSearchTheUserMeantIsRememberedAndCanBeRemoved() =
        runTest {
            val db = TestDb()
            val repo =
                com.tiagohs.hqr.data.repository
                    .RoomSearchRepository(db.db)
            val md = Fake(SourceId("md"), found = listOf(manga("md", "x", 1)))
            val vm =
                track(
                    com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel(
                        SourceRegistry(listOf(md)),
                        db.mangas,
                        searches = repo,
                    ),
                )
            vm.type("nar")
            vm.type("naruto")
            vm.submit()
            vm.recent.first { it.isNotEmpty() } shouldBe listOf("naruto")
            vm.removeRecent("naruto")
            vm.recent.first { it.isEmpty() } shouldBe emptyList()
        }

    @Test
    fun RF116_feedOpensWithWhatItHadAndAsksAgainOnlyAfterThirtyMinutes() =
        runTest {
            val md = Fake(SourceId("md"), latest = listOf(manga("md", "a", 300)))
            val repo = Feed(listOf(FeedItem(1, md.id, null)))
            val cache = MemoryCache()
            val registry = SourceRegistry(listOf(md))
            track(FeedViewModel(repo, registry, TestDb().mangas, cache, cache.clock)).state.first { !it.loading && !it.refreshing }
            md.asked shouldBe 1
            md.latest = listOf(manga("md", "b", 400))
            cache.now += 29 * 60 * 1_000L
            val soon =
                track(
                    FeedViewModel(repo, registry, TestDb().mangas, cache, cache.clock),
                ).state.first { !it.loading && !it.refreshing }
            soon.entries.map { it.manga.key } shouldContainExactly listOf("a")
            soon.updatedAtEpochMs shouldBe 1_790_000_000_000
            md.asked shouldBe 1
            cache.now += 2 * 60 * 1_000L
            val later = track(FeedViewModel(repo, registry, TestDb().mangas, cache, cache.clock))
            later.state.first {
                !it.loading &&
                    !it.refreshing &&
                    it.entries
                        .firstOrNull()
                        ?.manga
                        ?.key == "b"
            }
            md.asked shouldBe 2
        }

    @Test
    fun RF116_aFailedItemKeepsWhatItHadAndSaysSo() =
        runTest {
            val md = Fake(SourceId("md"), latest = listOf(manga("md", "a", 300)))
            val repo = Feed(listOf(FeedItem(1, md.id, null)))
            val cache = MemoryCache()
            val vm = track(FeedViewModel(repo, SourceRegistry(listOf(md)), TestDb().mangas, cache, cache.clock))
            vm.state.first { !it.loading && !it.refreshing && it.entries.isNotEmpty() }
            md.fails = true
            vm.refresh()
            val state = vm.state.first { !it.refreshing && it.failed.isNotEmpty() }
            state.entries.map { it.manga.key } shouldContainExactly listOf("a")
            state.failed shouldBe mapOf("MD" to ErrorKind.SERVER)
        }

    @Test
    fun RF115_theStarPutsTheSourceInTheFeedAndTakesItOut() =
        runTest {
            val repo = Feed((1L..4L).map { FeedItem(it, SourceId("s$it"), null) })
            val favorites =
                com.tiagohs.hqr.presentation.feed
                    .FavoriteSources(repo)
            favorites.toggle(SourceId("md")) shouldBe com.tiagohs.hqr.presentation.feed.FavoriteSources.Result.ADDED
            favorites.observe().first() shouldBe setOf(SourceId("s1"), SourceId("s2"), SourceId("s3"), SourceId("s4"), SourceId("md"))
            favorites.toggle(SourceId("hq")) shouldBe com.tiagohs.hqr.presentation.feed.FavoriteSources.Result.FEED_FULL
            favorites.toggle(SourceId("md")) shouldBe com.tiagohs.hqr.presentation.feed.FavoriteSources.Result.REMOVED
            favorites.observe().first().contains(SourceId("md")) shouldBe false
        }
}
