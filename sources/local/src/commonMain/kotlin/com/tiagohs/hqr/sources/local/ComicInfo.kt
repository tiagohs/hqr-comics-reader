package com.tiagohs.hqr.sources.local

/** The fields of a `ComicInfo.xml` HQR uses (RF-14). */
data class ComicInfo(
    val title: String? = null,
    val series: String? = null,
    val number: String? = null,
    val writers: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val summary: String? = null,
) {
    /** "Saga #1" with series and number; else the title. */
    val label: String?
        get() =
            when {
                series != null && number != null -> "$series #$number"
                else -> title ?: series
            }

    companion object {
        const val FILE_NAME = "comicinfo.xml"

        /** Reads the flat tags of the format; anything else in the file is ignored. */
        fun parse(xml: String): ComicInfo {
            fun tag(name: String): String? =
                Regex("<$name>(.*?)</$name>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .find(xml)
                    ?.groupValues
                    ?.get(1)
                    ?.let(::unescape)
                    ?.trim()
                    ?.ifEmpty { null }

            fun people(vararg names: String) =
                names
                    .flatMap { tag(it)?.split(',').orEmpty() }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            return ComicInfo(
                title = tag("Title"),
                series = tag("Series"),
                number = tag("Number"),
                writers = people("Writer"),
                artists = people("Penciller", "Inker", "CoverArtist"),
                summary = tag("Summary"),
            )
        }

        private val entity = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);")

        private fun unescape(text: String): String =
            entity.replace(text.replace(Regex("<!\\[CDATA\\[(.*?)]]>", RegexOption.DOT_MATCHES_ALL), "$1")) {
                when (val e = it.groupValues[1]) {
                    "amp" -> "&"
                    "lt" -> "<"
                    "gt" -> ">"
                    "quot" -> "\""
                    "apos" -> "'"
                    else ->
                        (if (e.startsWith("#x")) e.drop(2).toIntOrNull(HEX) else e.drop(1).toIntOrNull())
                            ?.let { code -> codePointToString(code) } ?: it.value
                }
            }

        private fun codePointToString(code: Int): String =
            if (code < SUPPLEMENTARY) {
                code.toChar().toString()
            } else {
                val c = code - SUPPLEMENTARY
                charArrayOf((HIGH + (c shr TEN_BITS)).toChar(), (LOW + (c and LOW_MASK)).toChar()).concatToString()
            }

        private const val HEX = 16
        private const val SUPPLEMENTARY = 0x10000
        private const val HIGH = 0xD800
        private const val LOW = 0xDC00
        private const val TEN_BITS = 10
        private const val LOW_MASK = 0x3FF
    }
}
