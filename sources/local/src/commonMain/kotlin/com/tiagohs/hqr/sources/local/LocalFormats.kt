package com.tiagohs.hqr.sources.local

/** Formats the local source understands (RF-13). CBR and CB7 depend on the platform (ADR-011). */
enum class LocalFormat(
    val extensions: Set<String>,
) {
    CBZ(setOf("cbz", "zip")),
    CBR(setOf("cbr", "rar")),
    CB7(setOf("cb7", "7z")),
    PDF(setOf("pdf")),
    ;

    companion object {
        private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp")

        fun of(name: String): LocalFormat? {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions }
        }

        fun isImage(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in imageExtensions
    }
}

/** An image of the comic inside an archive: macOS metadata and hidden files skipped. */
fun isPage(name: String): Boolean =
    LocalFormat.isImage(name) && !name.startsWith("__MACOSX/") && !name.substringAfterLast('/').startsWith(".")

/** Natural order: "page 2" before "page 10" (pages and files are named by people, not padded). */
object NaturalOrder : Comparator<String> {
    private val chunk = Regex("\\d+|\\D+")

    override fun compare(
        a: String,
        b: String,
    ): Int {
        val left = chunk.findAll(a.lowercase()).map { it.value }.toList()
        val right = chunk.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(left.size, right.size)) {
            val l = left[i]
            val r = right[i]
            val result =
                if (l[0].isDigit() && r[0].isDigit()) {
                    l
                        .trimStart('0')
                        .length
                        .compareTo(r.trimStart('0').length)
                        .takeIf { it != 0 }
                        ?: l.trimStart('0').compareTo(r.trimStart('0'))
                } else {
                    l.compareTo(r)
                }
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }
}
