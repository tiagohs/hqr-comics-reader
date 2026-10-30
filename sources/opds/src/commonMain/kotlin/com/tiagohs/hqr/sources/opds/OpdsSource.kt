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
import com.tiagohs.hqr.sources.local.FileTree
import com.tiagohs.hqr.sources.local.ZipReader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import io.ktor.util.encodeBase64
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One OPDS server as a source (RF-17, RF-18): its catalog is browsed feed by feed; the feed that holds books is a work
 * and each book a chapter. Pages come by OPDS-PSE when the server offers it, else from the CBZ downloaded once and
 * kept in the [BookStore]. The login goes only to the server's own origin, never through a redirect elsewhere (RF-95).
 */
@Suppress("TooManyFunctions") // The source contract plus the HTTP rules of RF-95 in one place.
class OpdsSource(
    val server: Server,
    http: HttpClient,
    private val password: suspend () -> String?,
    private val books: BookStore = MemoryBookStore(),
    private val maxFileBytes: Long = MAX_FILE_BYTES,
) : CatalogBrowser,
    SearchSource,
    ImageSource {
    override val id: SourceId = server.id
    override val name: String = server.name
    override val languages: List<String> = emptyList()

    // Redirects are followed by hand, so the credential is dropped when the origin changes (RF-95).
    private val client = http.config { followRedirects = false }
    private val origin = origin(server.url)
    private val lock = Mutex()
    private val downloading = HashMap<String, Deferred<FileTree>>()

    // The books of the works opened in this run, by entry id: pages find their stream or file here.
    private val known = HashMap<String, OpdsEntry>()

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
            url = feedUrl,
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
        val found = ArrayList<OpdsEntry>()
        var next: String? = manga.key
        var pages = 0
        while (next != null && pages++ < MAX_FEED_PAGES) {
            val feed = load(next)
            found += feed.entries.filter { it.isBook }
            next = feed.next
        }
        // The entry id is the key: it stays when the server rebuilds the file or its pages (RF-90).
        lock.withLock { found.forEach { known[it.id] = it } }
        return found.mapIndexed { i, book ->
            SourceChapter(key = book.id, number = (i + 1).toDouble(), title = book.title, pageCount = book.stream?.pageCount)
        }
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val book = lock.withLock { known[chapter.key] } ?: chapters(manga).let { lock.withLock { known[chapter.key] } }
        book ?: throw SourceException("Livro fora do servidor", SourceException.Kind.NOT_FOUND)
        val stream = book.stream
        if (stream != null) {
            return (0 until stream.pageCount!!).map { i ->
                SourcePage(i, stream.href.replace("{pageNumber}", i.toString()).replace("{maxWidth}", MAX_WIDTH.toString()))
            }
        }
        val href = book.acquisition!!.href
        val count = ZipReader(file(href), BookStore.PATH).images().size
        if (count == 0) throw SourceException("Arquivo sem imagens (só CBZ pelo servidor)", SourceException.Kind.PARSE)
        return (0 until count).map { i -> SourcePage(i, "$FILE_SCHEME$i|$href") }
    }

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        if (!url.startsWith(FILE_SCHEME)) return get(url).bodyAsBytes()
        val index = url.removePrefix(FILE_SCHEME).substringBefore('|').toInt()
        val reader = ZipReader(file(url.substringAfter('|')), BookStore.PATH)
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
        val description = parseOrNotOpds { Xml.parse(get(link.href).bodyAsText()) }
        val urls = description.all("Url")
        val atom = urls.firstOrNull { it.attr("type")?.contains("atom") == true } ?: urls.firstOrNull() ?: return null
        // Optional parameters ({startPage?}) are left empty.
        return atom.attr("template")?.replace(OPTIONAL_PARAM, "")?.let { OpdsFeed.resolve(link.href, it) }
    }

    /** The book file, downloaded once even when several pages ask at the same time; other files never wait for it. */
    private suspend fun file(href: String): FileTree {
        books.open(href)?.let { return it }
        val (pending, owner) =
            lock.withLock {
                downloading[href]?.let { it to false } ?: CompletableDeferred<FileTree>().also { downloading[href] = it }.let { it to true }
            }
        if (owner) {
            val deferred = pending as CompletableDeferred<FileTree>
            try {
                deferred.complete(download(href))
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Throwable,
            ) {
                // Every waiting page gets the same failure.
                deferred.completeExceptionally(e)
                throw e
            } finally {
                withContext(NonCancellable) { lock.withLock { downloading.remove(href) } }
            }
        }
        return pending.await()
    }

    private suspend fun download(href: String): FileTree = networkCall { certificateGuard { fetchFile(href) } }

    // Redirects, size limit and the streamed copy: one loop, read top to bottom.
    @Suppress("CyclomaticComplexMethod")
    private suspend fun fetchFile(href: String): FileTree {
        var current = href
        var hops = 0
        while (true) {
            val target = current
            val secret = credentialFor(target)
            var redirect: String? = null
            val tree =
                client.prepareGet(target) { secret?.let { header(HttpHeaders.Authorization, it) } }.execute { response ->
                    val location = response.headers[HttpHeaders.Location]
                    if (response.status.value in REDIRECTS && location != null) {
                        redirect = OpdsFeed.resolve(target, location)
                        return@execute null
                    }
                    response.ensureOk()
                    val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    if (declared != null && declared > maxFileBytes) throw tooBig()
                    val channel = response.bodyAsChannel()
                    books.save(href) { sink ->
                        val buffer = ByteArray(CHUNK)
                        var total = 0L
                        while (true) {
                            val n = channel.readAvailable(buffer, 0, buffer.size)
                            if (n < 0) break
                            total += n
                            if (total > maxFileBytes) return@save false
                            if (n > 0) sink(buffer, n)
                        }
                        true
                    } ?: throw tooBig()
                }
            if (tree != null) return tree
            if (redirect == null ||
                hops++ >= MAX_REDIRECTS
            ) {
                throw SourceException("Redirecionamento sem fim", SourceException.Kind.SERVER)
            }
            current = redirect!!
        }
    }

    private fun tooBig() = SourceException("Arquivo grande demais para ler pelo servidor", SourceException.Kind.PARSE)

    /** The login only for the server's own origin: same scheme, host and port (RF-95). */
    private suspend fun credentialFor(url: String): String? {
        val user = server.username ?: return null
        if (origin(url) != origin) return null
        return password()?.takeIf { it.isNotEmpty() }?.let { basic(user, it) }
    }

    private suspend fun load(url: String): OpdsFeed {
        val text = get(url).bodyAsText()
        return parseOrNotOpds { OpdsFeed.parse(text, url) }
    }

    /** GET with the login for the server's origin only; redirects followed by hand, up to five. */
    private suspend fun get(url: String): HttpResponse =
        networkCall {
            certificateGuard {
                var current = url
                var response: HttpResponse
                var hops = 0
                while (true) {
                    val secret = credentialFor(current)
                    response =
                        client.get(current) {
                            if (secret != null) header(HttpHeaders.Authorization, secret)
                        }
                    val location = response.headers[HttpHeaders.Location]
                    if (response.status.value !in REDIRECTS || location == null || hops++ >= MAX_REDIRECTS) break
                    current = OpdsFeed.resolve(current, location)
                }
                response.ensureOk()
            }
        }

    companion object {
        private const val CHUNK = 64 * 1024
        const val MAX_FILE_BYTES = 400L * 1024 * 1024
        private val OPTIONAL_PARAM = Regex("\\{[^}]*\\?\\}")
        const val FILE_SCHEME = "opds-file:"
        private const val MAX_WIDTH = 1600
        private const val MAX_FEED_PAGES = 50
        private const val MAX_REDIRECTS = 5
        private val REDIRECTS = 300..399

        /** scheme://host:port, the port always written. */
        fun origin(url: String): String = Url(url).let { "${it.protocol.name}://${it.host}:${it.port}" }

        fun basic(
            user: String,
            password: String,
        ): String = "Basic " + "$user:$password".encodeToByteArray().encodeBase64()
    }
}

/** Any failure to read the XML is "not OPDS", never a crash (RF-19, RNF-26). */
internal inline fun <T> parseOrNotOpds(block: () -> T): T =
    try {
        block()
    } catch (e: SourceException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: RuntimeException,
    ) {
        throw SourceException("Não é um catálogo OPDS", SourceException.Kind.PARSE, e)
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
