package com.tiagohs.hqr.presentation.settings

/** A content language as people know it, and the codes the sources use for it (MangaDex: "es" and "es-la"). */
data class ContentLanguage(
    val label: String,
    val codes: Set<String>,
)

/** Each one in its own name (RF-81), like the app language (RF-77). */
val CONTENT_LANGUAGES =
    listOf(
        ContentLanguage("Português (BR)", setOf("pt-br")),
        ContentLanguage("English", setOf("en")),
        ContentLanguage("Español", setOf("es", "es-la")),
        ContentLanguage("Français", setOf("fr")),
        ContentLanguage("Italiano", setOf("it")),
        ContentLanguage("日本語", setOf("ja")),
    )
