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
    ) : SourceFilter

    /** One or more values from a fixed list (status, demographic, original language). */
    @Serializable
    data class Choice(
        override val key: String,
        val options: List<Option>,
        val selected: Set<String> = emptySet(),
        val multiple: Boolean = true,
    ) : SourceFilter

    /** Free text, such as the year. */
    @Serializable
    data class Text(
        override val key: String,
        val value: String = "",
    ) : SourceFilter

    /** Ordering of the results. */
    @Serializable
    data class Sort(
        override val key: String,
        val options: List<Option>,
        val selected: String? = null,
        val ascending: Boolean = false,
    ) : SourceFilter

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
}
