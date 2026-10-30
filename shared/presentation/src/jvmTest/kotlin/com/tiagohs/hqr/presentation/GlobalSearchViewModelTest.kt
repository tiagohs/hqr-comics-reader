package com.tiagohs.hqr.presentation

import androidx.lifecycle.SavedStateHandle
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel
import com.tiagohs.hqr.presentation.browse.GlobalSearchViewModel.GroupState
import com.tiagohs.hqr.presentation.common.ErrorKind
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class GlobalSearchViewModelTest : MainDispatcherTest() {
    private val db = TestDb()
    private val mangaDex = FakeSource(SourceId("md"))
    private val hqNow = FakeSource(SourceId("hq"))
    private val registry = SourceRegistry(listOf(mangaDex, hqNow))

    private fun vm(saved: SavedStateHandle = SavedStateHandle()) =
        track(GlobalSearchViewModel(registry, db.mangas, saved, debounce = 50.milliseconds))

    @Test
    fun RF23_typingWaitsForThePauseAndAsksOncePerSource() =
        runTest {
            val vm = vm()
            vm.type("o")
            vm.type("on")
            vm.type("one piece")
            val done = vm.state.first { it.searched == "one piece" && !it.searching }
            done.groups.map { it.sourceId.value } shouldContainExactly listOf("md", "hq")
            mangaDex.requested shouldContainExactly listOf("s:one piece:1")
            hqNow.requested shouldContainExactly listOf("s:one piece:1")
        }

    @Test
    fun RF23_RF05_aBrokenSourceKeepsItsErrorAndTheOtherShowsResults() =
        runTest {
            hqNow.fail = SourceException.Kind.SERVER
            val vm = vm()
            vm.type("one piece")
            vm.submit()
            val state = vm.state.first { it.searched == "one piece" && !it.searching }
            state.groups[0]
                .state
                .shouldBeInstanceOf<GroupState.Results>()
                .items.size shouldBe 2
            state.groups[1].state shouldBe GroupState.Error(ErrorKind.SERVER)
            hqNow.fail = null
            vm.retry(SourceId("hq"))
            vm.state
                .first { it.groups[1].state is GroupState.Results }
                .groups[1]
                .state
                .shouldBeInstanceOf<GroupState.Results>()
                .items
                .size shouldBe 2
        }

    @Test
    fun RF23_aSlowSourceDoesNotHoldTheOthers() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            hqNow.searchGate = gate
            val vm = vm()
            vm.type("berserk")
            val partial = vm.state.first { it.groups.firstOrNull()?.state is GroupState.Results }
            partial.groups[1].state shouldBe GroupState.Loading
            gate.complete(Unit)
            vm.state.first { !it.searching && it.searched == "berserk" }
        }

    @Test
    fun RF28_nothingFoundAnywhere() =
        runTest {
            mangaDex.searchFindsNothing = true
            hqNow.searchFindsNothing = true
            val vm = vm()
            vm.type("xyzq")
            vm.state.first { it.nothingFound }.searched shouldBe "xyzq"
        }

    @Test
    fun RF26_resultsAlreadyInTheLibraryAreMarked() =
        runTest {
            val first = mangaDex.search("frieren", emptyList(), 1).items.first()
            mangaDex.requested.clear()
            db.mangas.setInLibrary(db.mangas.upsert(first).id, true)
            val vm = vm()
            vm.type("frieren")
            vm.state
                .first { !it.searching && it.searched == "frieren" && it.groups[0].inLibrary.isNotEmpty() }
                .groups[0]
                .inLibrary shouldBe setOf(first.key)
        }

    @Test
    fun RF88_theQuerySurvivesTheProcess() =
        runTest {
            val saved = SavedStateHandle()
            vm(saved).type("akira")
            val again = vm(saved)
            again.state.first { it.searched == "akira" && !it.searching }.query shouldBe "akira"
        }

    @Test
    fun RF23_searchKeyAsksAgainOnlyTheSourcesThatFailed() =
        runTest {
            hqNow.fail = SourceException.Kind.NETWORK
            val vm = vm()
            vm.type("akira")
            vm.submit()
            vm.state.first { !it.searching && it.searched == "akira" }
            hqNow.fail = null
            vm.submit()
            vm.state.first { it.groups[1].state is GroupState.Results }
            mangaDex.requested shouldContainExactly listOf("s:akira:1")
        }

    @Test
    fun RF23_debounceWaits400Milliseconds() =
        runTest {
            val vm = track(GlobalSearchViewModel(registry, db.mangas))
            vm.type("berserk")
            Thread.sleep(150) // real time: the view model runs on a real Main thread
            mangaDex.requested shouldBe emptyList()
            vm.state.first { it.searched == "berserk" && !it.searching }
            mangaDex.requested shouldContainExactly listOf("s:berserk:1")
        }

    @Test
    fun RF52_longPressOnAResultAndUndo() =
        runTest {
            val vm = vm()
            vm.type("kaiju")
            val item =
                vm.state
                    .first { it.groups.firstOrNull()?.state is GroupState.Results }
                    .groups[0]
                    .state
                    .shouldBeInstanceOf<GroupState.Results>()
                    .items
                    .first()
            vm.toggleLibrary(item) shouldBe null
            vm.state.first { item.key in it.groups[0].inLibrary }
            vm.undoToggle(item, null)
            vm.state.first { item.key !in it.groups[0].inLibrary }
        }
}
