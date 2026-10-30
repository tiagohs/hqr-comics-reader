package com.tiagohs.hqr.sources.opds

import io.ktor.http.Url

/** An Atom link: `rel`, `type`, the address already absolute, and the PSE page count when there is one. */
internal data class OpdsLink(
    val rel: String?,
    val type: String?,
    val href: String,
    val pageCount: Int? = null,
) {
    val isAcquisition: Boolean get() = rel?.startsWith(ACQUISITION) == true
    val isStream: Boolean get() = rel == STREAM
    val isImage: Boolean get() = rel == IMAGE || rel == THUMBNAIL || rel == IMAGE_OLD || rel == THUMBNAIL_OLD
    val isNavigation: Boolean
        get() = type?.contains("profile=opds-catalog") == true || type?.startsWith("application/atom+xml") == true && !isAcquisition

    companion object {
        const val ACQUISITION = "http://opds-spec.org/acquisition"
        const val STREAM = "http://vaemendis.net/opds-pse/stream"
        const val IMAGE = "http://opds-spec.org/image"
        const val THUMBNAIL = "http://opds-spec.org/image/thumbnail"
        const val IMAGE_OLD = "http://opds-spec.org/cover"
        const val THUMBNAIL_OLD = "http://opds-spec.org/thumbnail"
    }
}

internal data class OpdsEntry(
    val id: String,
    val title: String,
    val summary: String?,
    val authors: List<String>,
    val links: List<OpdsLink>,
) {
    val stream: OpdsLink? get() = links.firstOrNull { it.isStream && it.pageCount != null }
    val acquisition: OpdsLink? get() = links.firstOrNull { it.isAcquisition }
    val isBook: Boolean get() = stream != null || acquisition != null
    val navigation: OpdsLink? get() =
        links.firstOrNull { it.isNavigation && it.rel != "alternate" }
            ?: links.firstOrNull { it.isNavigation }
    val cover: String?
        get() =
            (links.firstOrNull { it.rel == OpdsLink.THUMBNAIL || it.rel == OpdsLink.THUMBNAIL_OLD } ?: links.firstOrNull { it.isImage })
                ?.href
}

internal data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry>,
    val links: List<OpdsLink>,
) {
    val next: String? get() = links.firstOrNull { it.rel == "next" }?.href
    val search: OpdsLink? get() = links.firstOrNull { it.rel == "search" }

    companion object {
        /** Parses an Atom feed; anything else is not OPDS (RF-19). */
        fun parse(
            text: String,
            base: String,
        ): OpdsFeed {
            val root = Xml.parse(text)
            require(root.local == "feed") { "não é um feed Atom" }
            return OpdsFeed(
                title = root.textOf("title").orEmpty(),
                entries =
                    root.all("entry").map { e ->
                        OpdsEntry(
                            id = e.textOf("id") ?: e.textOf("title").orEmpty(),
                            title = e.textOf("title").orEmpty(),
                            summary = e.textOf("summary") ?: e.textOf("content"),
                            authors = e.all("author").mapNotNull { it.textOf("name") },
                            links = e.all("link").mapNotNull { link(it, base) },
                        )
                    },
                links = root.all("link").mapNotNull { link(it, base) },
            )
        }

        private fun link(
            node: XmlNode,
            base: String,
        ): OpdsLink? {
            val href = node.attr("href")?.takeIf { it.isNotBlank() } ?: return null
            return OpdsLink(node.attr("rel"), node.attr("type"), resolve(base, href), node.attr("count")?.toIntOrNull())
        }

        /** A relative address made absolute against the feed's; templates such as `{pageNumber}` stay as they are. */
        fun resolve(
            base: String,
            href: String,
        ): String {
            if (href.startsWith("http://") || href.startsWith("https://")) return href
            val b = Url(base)
            val origin = "${b.protocol.name}://${b.hostWithPortIfNeeded()}"
            return when {
                href.startsWith("//") -> "${b.protocol.name}:$href"
                href.startsWith("/") -> origin + href
                else -> origin + b.encodedPath.substringBeforeLast('/', "") + "/" + href
            }
        }

        private fun Url.hostWithPortIfNeeded(): String = if (port == protocol.defaultPort) host else "$host:$port"
    }
}
