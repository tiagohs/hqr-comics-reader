package com.tiagohs.hqr.sources.opds

import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogBrowser
import com.tiagohs.hqr.domain.source.CatalogEntry
import com.tiagohs.hqr.domain.source.CatalogPage
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import com.tiagohs.hqr.sources.local.FileNode
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.ZipReader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import io.ktor.util.encodeBase64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One OPDS server as a source (RF-17, RF-18): its catalog is browsed feed by feed; the feed that holds books is a work
 * and each book a chapter. Pages come by OPDS-PSE when the server offers it, else from the CBZ downloaded once and
 * kept in memory. The login goes only to the server's own host, never through a redirect elsewhere (RF-95).
 */
class OpdsSource(
    val server: Server,
    http: HttpClient,
    private val password: suspend () -> String?,
) : CatalogBrowser,
    SearchSource,
    ImageSource {
    override val id: SourceId = server.id
    override val name: String = server.name
    override val languages: List<String> = emptyList()

    // Redirects are followed by hand, so the credential can be dropped when the host changes.
    private val client = http.config { followRedirects = false }
    private val host = Url(server.url).host
    private val files = LinkedHashMap<String, ByteArray>()
    private val filesLock = Mutex()

    override suspend fun feed(url: String?): CatalogPage {
        val feedUrl = url ?: server.url
        val feed = load(feedUrl)
        return CatalogPage(
            title = feed.title.ifBlank { server.name },
            entries =
                feed.entries.mapNotNull { e ->
                    when {
                        e.isBook -> CatalogEntry(e.title, feedUrl, isBook = true, coverUrl = e.cover, subtitle = e.authors.joinToString())
                        e.navigation != null ->
                            CatalogEntry(
                                e.title,
                                e.navigation!!.href,
                                isBook = false,
                                coverUrl = e.cover,
                                subtitle = e.summary,
                            )
                        else -> null
                    }
                },
            nextUrl = feed.next,
            canSearch = feed.search != null,
        )
    }

    override suspend fun searchCatalog(query: String): CatalogPage {
        val template = searchTemplate(load(server.url)) ?: throw SourceException("Servidor sem busca", SourceException.Kind.UNAVAILABLE)
        return feed(template.replace("{searchTerms}", query.trim().encodeURLParameter()))
    }

    override fun work(
        feedUrl: String,
        title: String,
    ): SourceManga = SourceManga(id, feedUrl, title, url = feedUrl, kind = ReadingKind.COMIC)

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        if (page > 1 || query.isBlank()) return MangaPage(emptyList(), false)
        // Search results become works: the folders they open, or the result feed for loose books.
        val result = searchCatalog(query)
        val works =
            result.entries
                .filter { !it.isBook }
                .map { SourceManga(id, it.url, it.title, coverUrl = it.coverUrl, url = it.url, kind = ReadingKind.COMIC) }
        return MangaPage(works, false)
    }

    override suspend fun details(manga: SourceManga): SourceManga {
        val feed = load(manga.key)
        val first = feed.entries.firstOrNull { it.isBook }
        return manga.copy(
            title = feed.title.ifBlank { manga.title },
            coverUrl = manga.coverUrl ?: first?.cover,
            authors = first?.authors.orEmpty(),
            description = feed.entries.firstOrNull()?.summary,
        )
    }

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val books = ArrayList<OpdsEntry>()
        var next: String? = manga.key
        var pages = 0
        while (next != null && pages++ < MAX_FEED_PAGES) {
            val feed = load(next)
            books += feed.entries.filter { it.isBook }
            next = feed.next
        }
        return books.mapIndexed { i, book ->
            val stream = book.stream
            val key =
                if (stream != null) {
                    "$STREAM_KEY${stream.pageCount}|${stream.href}"
                } else {
                    "$FILE_KEY${book.acquisition!!.href}"
                }
            SourceChapter(key = key, number = (i + 1).toDouble(), title = book.title, pageCount = stream?.pageCount)
        }
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        if (chapter.key.startsWith(STREAM_KEY)) {
            val count =
                chapter.key
                    .removePrefix(STREAM_KEY)
                    .substringBefore('|')
                    .toInt()
            val template = chapter.key.substringAfter('|')
            return (0 until count).map { i ->
                SourcePage(i, template.replace("{pageNumber}", i.toString()).replace("{maxWidth}", MAX_WIDTH.toString()))
            }
        }
        val href = chapter.key.removePrefix(FILE_KEY)
        val names = zip(href).images().map { it.name }
        if (names.isEmpty()) throw SourceException("Arquivo sem imagens (só CBZ pelo servidor)", SourceException.Kind.PARSE)
        return names.mapIndexed { i, entry -> SourcePage(i, "$FILE_SCHEME$i|$href") }
    }

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        if (!url.startsWith(FILE_SCHEME)) return get(url).bodyAsBytes()
        val index = url.removePrefix(FILE_SCHEME).substringBefore('|').toInt()
        val reader = zip(url.substringAfter('|'))
        return reader.read(reader.images()[index])
    }

    /** Connection check of RF-19: the root answers as an Atom feed with this login. */
    suspend fun check() {
        load(server.url)
    }

    private suspend fun searchTemplate(root: OpdsFeed): String? {
        val link = root.search ?: return null
        if (link.href.contains("{searchTerms}")) return link.href
        // An OpenSearch description: its Atom template.
        val description = Xml.parse(get(link.href).bodyAsText())
        val urls = description.all("Url")
        val atom = urls.firstOrNull { it.attr("type")?.contains("atom") == true } ?: urls.firstOrNull() ?: return null
        return atom.attr("template")?.let { OpdsFeed.resolve(link.href, it) }
    }

    private suspend fun zip(href: String): ZipReader {
        val bytes =
            filesLock.withLock {
                files[href] ?: get(href).bodyAsBytes().also { downloaded ->
                    files[href] = downloaded
                    // Two files at most in memory: the chapter being read and the next.
                    while (files.size > KEPT_FILES) files.remove(files.keys.first())
                }
            }
        return ZipReader(MemoryTree(bytes), MemoryTree.PATH)
    }

    private suspend fun load(url: String): OpdsFeed {
        val text = get(url).bodyAsText()
        return try {
            OpdsFeed.parse(text, url)
        } catch (e: IllegalArgumentException) {
            throw SourceException("Não é um catálogo OPDS", SourceException.Kind.PARSE, e)
        } catch (e: IllegalStateException) {
            throw SourceException("Não é um catálogo OPDS", SourceException.Kind.PARSE, e)
        }
    }

    /** GET with the login for the server's host only; redirects followed by hand, up to five. */
    private suspend fun get(url: String): HttpResponse =
        networkCall {
            certificateGuard {
                var current = url
                var response: HttpResponse
                var hops = 0
                while (true) {
                    val target = Url(current)
                    val secret = if (target.host == host && server.username != null) password() else null
                    response =
                        client.get(current) {
                            if (secret != null) header(HttpHeaders.Authorization, basic(server.username!!, secret))
                        }
                    val location = response.headers[HttpHeaders.Location]
                    if (response.status.value !in REDIRECTS || location == null || hops++ >= MAX_REDIRECTS) break
                    current = OpdsFeed.resolve(current, location)
                }
                response.ensureOk()
            }
        }

    private class MemoryTree(
        private val bytes: ByteArray,
    ) : FileTree {
        override suspend fun isReady() = true

        override suspend fun children(path: String) = emptyList<FileNode>()

        override suspend fun size(path: String) = bytes.size.toLong()

        override suspend fun read(
            path: String,
            offset: Long,
            length: Int,
        ): ByteArray = bytes.copyOfRange(offset.toInt(), minOf(bytes.size, offset.toInt() + length))

        companion object {
            const val PATH = "book.cbz"
        }
    }

    companion object {
        private const val STREAM_KEY = "pse:"
        private const val FILE_KEY = "file:"
        const val FILE_SCHEME = "opds-file:"
        private const val MAX_WIDTH = 1600
        private const val MAX_FEED_PAGES = 50
        private const val KEPT_FILES = 2
        private const val MAX_REDIRECTS = 5
        private val REDIRECTS = 300..399

        fun basic(
            user: String,
            password: String,
        ): String = "Basic " + "$user:$password".encodeToByteArray().encodeBase64()
    }
}

/** TLS failures become their own kind, so the screen can say "certificado inválido" (RF-19). */
internal suspend inline fun <T> certificateGuard(block: () -> T): T =
    try {
        block()
    } catch (e: SourceException) {
        throw e
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        val text = generateSequence<Throwable>(e) { it.cause }.joinToString(" ") { "${it::class.simpleName} ${it.message}" }
        if (CERTIFICATE_WORDS.any { text.contains(it, ignoreCase = true) }) {
            throw SourceException("Certificado inválido", SourceException.Kind.CERTIFICATE, e)
        }
        throw e
    }

private val CERTIFICATE_WORDS = listOf("certificate", "SSLHandshake", "PKIX", "CertPath", "trust anchor")
