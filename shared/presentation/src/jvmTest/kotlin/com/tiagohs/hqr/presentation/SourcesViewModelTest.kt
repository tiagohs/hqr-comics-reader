package com.tiagohs.hqr.presentation

import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SourceRegistry
import com.tiagohs.hqr.domain.source.SourceSwitches
import com.tiagohs.hqr.presentation.sources.SourcesViewModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SourcesViewModelTest : MainDispatcherTest() {
    private val registry = SourceRegistry(listOf(FakeSource(SourceId("md")), FakeSource(SourceId("hq")), FakeSource(SourceId.LOCAL)))
    private val switches = SourceSwitches { id, on -> registry.setUserDisabled(if (on) emptySet() else setOf(id)) }

    @Test
    fun RF03_switchTurnsASourceOffAndOn() =
        runTest {
            val vm = track(SourcesViewModel(registry, switches))
            vm.state
                .first { it.online.size == 2 }
                .local
                ?.id shouldBe SourceId.LOCAL
            vm.setEnabled(SourceId("hq"), false)
            vm.state.first { s -> s.online.any { it.id.value == "hq" && !it.on } }
            registry.active.value.map { it.id.value } shouldBe listOf("md", SourceId.LOCAL.value)
            vm.setEnabled(SourceId("hq"), true)
            vm.state.first { s -> s.online.all { it.on } }
        }

    @Test
    fun RF105_sourceTurnedOffByTheListStaysOff() =
        runTest {
            registry.setListDisabled(setOf(SourceId("md")))
            val vm = track(SourcesViewModel(registry, switches))
            val row =
                vm.state
                    .first { it.online.isNotEmpty() }
                    .online
                    .first { it.id.value == "md" }
            row.status shouldBe SourceRegistry.Status.OFF_BY_LIST
            vm.setEnabled(SourceId("md"), true)
            registry.status(SourceId("md")) shouldBe SourceRegistry.Status.OFF_BY_LIST
        }
}
