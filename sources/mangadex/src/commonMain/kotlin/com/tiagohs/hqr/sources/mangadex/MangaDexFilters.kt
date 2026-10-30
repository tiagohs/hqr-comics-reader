package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.domain.source.SourceFilter.Option
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter

/**
 * The search filters of the MangaDex API v5 (RF-22): tags included and excluded, status, demographic, original
 * language, year and order. Keys and values are the API's; the screen names them in the app language.
 */
object MangaDexFilters {
    const val TAGS = "tags"
    const val STATUS = "status"
    const val DEMOGRAPHIC = "demographic"
    const val ORIGINAL_LANGUAGE = "originalLanguage"
    const val YEAR = "year"
    const val ORDER = "order"
    const val RELEVANCE = "relevance"
    const val FOLLOWED = "followedCount"

    private fun options(vararg values: String) = values.map { Option(it, it) }

    fun defaults(tags: List<Option>): List<SourceFilter> =
        listOf(
            SourceFilter.Tags(TAGS, tags),
            SourceFilter.Choice(STATUS, options("ongoing", "completed", "hiatus", "cancelled")),
            SourceFilter.Choice(DEMOGRAPHIC, options("shounen", "shoujo", "seinen", "josei", "none")),
            SourceFilter.Choice(ORIGINAL_LANGUAGE, options("ja", "ko", "zh", "en", "pt-br")),
            SourceFilter.Text(YEAR),
            SourceFilter.Sort(
                ORDER,
                options(RELEVANCE, "latestUploadedChapter", FOLLOWED, "createdAt", "title", "year", "rating"),
                selected = RELEVANCE,
            ),
        )
}

/** Puts the filters in the request; without a title, "relevance" has nothing to rank and becomes "most followed". */
internal fun HttpRequestBuilder.applyFilters(
    filters: List<SourceFilter>,
    hasQuery: Boolean,
) {
    filters.forEach { filter ->
        when (filter) {
            is SourceFilter.Tags -> tags(filter)
            is SourceFilter.Choice -> choice(filter)
            is SourceFilter.Text -> year(filter)
            is SourceFilter.Sort -> Unit
        }
    }
    val sort = filters.filterIsInstance<SourceFilter.Sort>().firstOrNull()
    val field = sort?.selected?.takeUnless { it == MangaDexFilters.RELEVANCE && !hasQuery }
    val fallback = if (hasQuery) MangaDexFilters.RELEVANCE else MangaDexFilters.FOLLOWED
    parameter("order[${field ?: fallback}]", if (sort?.ascending == true) "asc" else "desc")
}

private fun HttpRequestBuilder.year(filter: SourceFilter.Text) {
    if (filter.key != MangaDexFilters.YEAR) return
    filter.value
        .trim()
        .toIntOrNull()
        ?.let { parameter("year", it) }
}

private fun HttpRequestBuilder.tags(filter: SourceFilter.Tags) {
    filter.included.forEach { parameter("includedTags[]", it) }
    filter.excluded.forEach { parameter("excludedTags[]", it) }
    if (filter.included.isNotEmpty()) parameter("includedTagsMode", "AND")
    if (filter.excluded.isNotEmpty()) parameter("excludedTagsMode", "OR")
}

private val choiceParameters =
    mapOf(
        MangaDexFilters.STATUS to "status[]",
        MangaDexFilters.DEMOGRAPHIC to "publicationDemographic[]",
        MangaDexFilters.ORIGINAL_LANGUAGE to "originalLanguage[]",
    )

private fun HttpRequestBuilder.choice(filter: SourceFilter.Choice) {
    val name = choiceParameters[filter.key] ?: return
    filter.selected.forEach { parameter(name, it) }
}
