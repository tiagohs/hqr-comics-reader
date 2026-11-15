package com.tiagohs.hqr.domain

import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.domain.source.SourceFilter.Option
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Editing a source's filters (RF-22), the same rules on every platform. */
class SourceFilterEditsTest {
    private val options = listOf(Option("r", "Romance"), Option("a", "Ação"))

    @Test
    fun RF22_aTagGoesFromOffToIncludedToExcludedAndBack() {
        val off = SourceFilter.Tags("tags", options)
        val included = off.cycle("r")
        included.included shouldBe setOf("r")
        val excluded = included.cycle("r")
        excluded.included shouldBe emptySet()
        excluded.excluded shouldBe setOf("r")
        excluded.cycle("r") shouldBe off
    }

    @Test
    fun RF22_aChoiceOfOneKeepsOnlyTheLastAndManyAddUp() {
        val one = SourceFilter.Choice("status", options, multiple = false)
        one.toggle("r").toggle("a").selected shouldBe setOf("a")
        one.toggle("r").toggle("r").selected shouldBe emptySet()
        SourceFilter
            .Choice("status", options)
            .toggle("r")
            .toggle("a")
            .selected shouldBe setOf("r", "a")
    }

    @Test
    fun RF22_theYearKeepsFourDigits() {
        SourceFilter.Text("year").typed("2o024x9").value shouldBe "2024"
    }

    @Test
    fun RF22_theOrderChangesWithItsDirection() {
        val sort = SourceFilter.Sort("order", options, selected = "r")
        sort.select("a").selected shouldBe "a"
        sort.direction(ascending = true).ascending shouldBe true
    }

    @Test
    fun RF22_aChangedFilterReplacesOnlyTheOneWithItsKey() {
        val tags = SourceFilter.Tags("tags", options)
        val year = SourceFilter.Text("year")
        SourceFilter.replace(listOf(tags, year), year.typed("1999")) shouldBe listOf(tags, SourceFilter.Text("year", "1999"))
    }
}
