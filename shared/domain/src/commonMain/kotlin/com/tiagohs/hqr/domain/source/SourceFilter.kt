package com.tiagohs.hqr.domain.source

import kotlinx.serialization.Serializable

/** A filter offered by a source (RF-22). Saved searches store them as JSON (RF-24). */
@Serializable
sealed interface SourceFilter {
    val key: String

    /** Tags that can be included, excluded or left alone (tri-state chips, SCR-EXP-003). */
    @Serializable
    data class Tags(
        override val key: String,
        val options: List<Option>,
        val included: Set<String> = emptySet(),
        val excluded: Set<String> = emptySet(),
    ) : SourceFilter {
        /** RF-22: off → included → excluded → off. */
        fun cycle(value: String): Tags =
            when (value) {
                in included -> copy(included = included - value, excluded = excluded + value)
                in excluded -> copy(excluded = excluded - value)
                else -> copy(included = included + value)
            }
    }

    /** One or more values from a fixed list (status, demographic, original language). */
    @Serializable
    data class Choice(
        override val key: String,
        val options: List<Option>,
        val selected: Set<String> = emptySet(),
        val multiple: Boolean = true,
    ) : SourceFilter {
        /** A chosen value goes out; a new one adds up, or takes the place of the other when only one is allowed. */
        fun toggle(value: String): Choice =
            copy(
                selected =
                    when {
                        value in selected -> selected - value
                        multiple -> selected + value
                        else -> setOf(value)
                    },
            )
    }

    /** Free text, such as the year. */
    @Serializable
    data class Text(
        override val key: String,
        val value: String = "",
    ) : SourceFilter {
        /** The free text the sources take is a year: digits only, four at most. */
        fun typed(text: String): Text = copy(value = text.filter { it.isDigit() }.take(YEAR_DIGITS))
    }

    /** Ordering of the results. */
    @Serializable
    data class Sort(
        override val key: String,
        val options: List<Option>,
        val selected: String? = null,
        val ascending: Boolean = false,
    ) : SourceFilter {
        fun select(value: String): Sort = copy(selected = value)

        fun direction(ascending: Boolean): Sort = copy(ascending = ascending)
    }

    /** True when the user changed nothing from the defaults the source gave. */
    val isDefault: Boolean
        get() =
            when (this) {
                is Tags -> included.isEmpty() && excluded.isEmpty()
                is Choice -> selected.isEmpty()
                is Text -> value.isBlank()
                is Sort -> true
            }

    @Serializable
    data class Option(
        val value: String,
        val label: String,
    )

    companion object {
        private const val YEAR_DIGITS = 4

        /** [changed] takes the place of the filter with its key. */
        fun replace(
            filters: List<SourceFilter>,
            changed: SourceFilter,
        ): List<SourceFilter> = filters.map { if (it.key == changed.key) changed else it }
    }
}
