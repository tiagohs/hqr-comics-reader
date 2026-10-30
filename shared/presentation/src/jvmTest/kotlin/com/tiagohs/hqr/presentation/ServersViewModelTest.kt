package com.tiagohs.hqr.presentation

import androidx.lifecycle.SavedStateHandle
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogBrowser
import com.tiagohs.hqr.domain.source.CatalogEntry
import com.tiagohs.hqr.domain.source.CatalogPage
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.ServerInput
import com.tiagohs.hqr.domain.source.ServerManager
import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import com.tiagohs.hqr.presentation.sources.AddServerViewModel
import com.tiagohs.hqr.presentation.sources.ServerCatalogViewModel
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ServersViewModelTest : MainDispatcherTest() {
    private val db = TestDb()

    private class FakeServers : ServerManager {
        val list = MutableStateFlow<List<Server>>(emptyList())
        var problem: ServerProblem? = null

        override fun observe() = list

        override suspend fun test(input: ServerInput) = problem

        override suspend fun save(
            input: ServerInput,
            id: SourceId?,
        ): ServerProblem? {
            problem?.let { return it }
            list.value =
                list.value.filter { it.id != id } + Server(id ?: SourceId("opds-1"), input.name, input.url, input.username.ifBlank { null })
            return null
        }

        override suspend fun delete(id: SourceId) {
            list.value = list.value.filter { it.id != id }
        }

        override suspend fun start() = Unit
    }

    private class FakeCatalog : CatalogBrowser {
        override val id = SourceId("opds-1")
        override val name = "Casa"
        override val languages = emptyList<String>()
        val asked = mutableListOf<String?>()

        override suspend fun feed(url: String?): CatalogPage {
            asked += url
            return when (url) {
                null -> CatalogPage("Casa", listOf(CatalogEntry("Séries", "s", isBook = false)), canSearch = true)
                "s" -> CatalogPage("Séries", listOf(CatalogEntry("Saga", "saga", isBook = false)), nextUrl = "s2")
                "s2" -> CatalogPage("Séries", listOf(CatalogEntry("Akira", "akira", isBook = false)))
                else -> CatalogPage("Saga", listOf(CatalogEntry("Saga 01", url, isBook = true)))
            }
        }

        override suspend fun searchCatalog(query: String) = CatalogPage("Busca", listOf(CatalogEntry("Saga", "saga", isBook = false)))

        override fun work(
            feedUrl: String,
            title: String,
        ) = SourceManga(id, feedUrl, title)

        override suspend fun details(manga: SourceManga) = manga

        override suspend fun chapters(manga: SourceManga) = emptyList<SourceChapter>()

        override suspend fun pages(
            manga: SourceManga,
            chapter: SourceChapter,
        ) = emptyList<SourcePage>()
    }

    @Test
    fun RF19_testShowsTheCauseAndSaveOnlyWhenItWorks() =
        runTest {
            val servers = FakeServers()
            val vm = track(AddServerViewModel(servers))
            vm.state.value.canSubmit shouldBe false
            vm.edit(ServerInput("Casa", "https://komga.local/opds/v1.2/catalog", "tiago", "errada"))
            servers.problem = ServerProblem.WRONG_LOGIN
            vm.test()
            vm.state.first { it.result != null }.result shouldBe AddServerViewModel.Result.Problem(ServerProblem.WRONG_LOGIN)
            vm.save()
            vm.state.first { !it.busy && it.result != null }.saved shouldBe false
            servers.list.value shouldBe emptyList()
            servers.problem = null
            vm.edit(
                vm.state.value.input
                    .copy(password = "certa"),
            )
            vm.state.value.result shouldBe null
            vm.save()
            vm.state.first { it.saved }
            servers.list.value
                .single()
                .name shouldBe "Casa"
        }

    @Test
    fun RF17_editingLoadsTheServerWithoutThePassword() =
        runTest {
            val servers = FakeServers()
            servers.list.value = listOf(Server(SourceId("opds-1"), "Casa", "https://k/opds", "tiago"))
            val vm = track(AddServerViewModel(servers, SourceId("opds-1")))
            val input = vm.state.first { it.input.name == "Casa" }.input
            input.username shouldBe "tiago"
            input.password shouldBe ""
            vm.state.value.isEdit shouldBe true
        }

    @Test
    fun RF17_serversAreListedApartAndCanBeRemoved() =
        runTest {
            val servers = FakeServers()
            val registry = SourceRegistry(listOf(FakeSource(SourceId("md")), FakeCatalog()))
            servers.list.value = listOf(Server(SourceId("opds-1"), "Casa", "https://k/opds", null))
            val vm = track(SourcesViewModel(registry, SourceSwitches { _, _ -> }, servers))
            val state = vm.state.first { it.servers.isNotEmpty() }
            state.online.map { it.id.value } shouldContainExactly listOf("md")
            vm.deleteServer(SourceId("opds-1"))
            vm.state.first { it.servers.isEmpty() }
        }

    @Test
    fun RF18_RF88_foldersBackSearchLoadMoreAndBookOpensItsWork() =
        runTest {
            val catalog = FakeCatalog()
            val registry = SourceRegistry(listOf(catalog))
            val saved = SavedStateHandle()
            val vm = track(ServerCatalogViewModel(catalog.id, registry, db.mangas, saved))
            val root = vm.state.first { !it.loading && it.page != null }
            root.canGoUp shouldBe false
            vm.open(root.page!!.entries.single()) {}
            val series = vm.state.first { !it.loading && it.page?.title == "Séries" }
            series.canGoUp shouldBe true
            vm.loadMore()
            vm.state.first { it.page?.entries?.size == 2 }
            // The path comes back after the process ends.
            val again = track(ServerCatalogViewModel(catalog.id, registry, db.mangas, saved))
            again.state.first { !it.loading && it.page?.title == "Séries" }
            vm.open(CatalogEntry("Saga", "saga", isBook = false)) {}
            val books = vm.state.first { !it.loading && it.page?.title == "Saga" }
            val opened = CompletableDeferred<Long>()
            vm.open(books.page!!.entries.single()) { opened.complete(it) }
            db.mangas
                .get(opened.await())!!
                .source.key shouldBe "saga"
            vm.up() shouldBe true
            vm.state.first { !it.loading && it.page?.title == "Séries" }
            vm.search("saga")
            vm.state.first { !it.loading && it.page?.title == "Busca" }
            vm.up() shouldBe true
            vm.up() shouldBe false
        }
}
