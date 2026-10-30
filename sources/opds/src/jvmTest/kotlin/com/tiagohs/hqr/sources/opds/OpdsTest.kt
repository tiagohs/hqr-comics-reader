package com.tiagohs.hqr.sources.opds

import com.tiagohs.hqr.data.db.inMemoryDatabase
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.source.SecretStore
import com.tiagohs.hqr.domain.source.Server
import com.tiagohs.hqr.domain.source.ServerInput
import com.tiagohs.hqr.domain.source.ServerProblem
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test

/** OPDS against a fake Komga: feeds, OpenSearch, PSE, CBZ, login and redirects (RF-17 to RF-19, RF-95). */
class OpdsTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var routes: (HttpRequestData) -> Triple<HttpStatusCode, Any, Map<String, String>>? = { null }
    private val http =
        HttpClient(
            MockEngine { request ->
                requests += request
                val (status, body, headers) =
                    routes(request) ?: Komga.answer(request.url.toString(), request.headers[HttpHeaders.Authorization])
                val h = headersOf(*headers.map { it.key to listOf(it.value) }.toTypedArray())
                when (body) {
                    is ByteArray -> respond(body, status, h)
                    else -> respond(body.toString(), status, h)
                }
            },
        )
    private val db = inMemoryDatabase()
    private val secrets =
        object : SecretStore {
            val map = HashMap<String, String>()

            override suspend fun get(key: String) = map[key]

            override suspend fun put(
                key: String,
                value: String,
            ) {
                map[key] = value
            }

            override suspend fun remove(key: String) {
                map.remove(key)
            }
        }
    private val registry = SourceRegistry(emptyList())
    private val servers = OpdsServers(db, secrets, registry, http)
    private val komga = Server(SourceId("opds-k"), "Komga", "${Komga.BASE}/opds/v1.2/catalog", "tiago")
    private val source = OpdsSource(komga, http) { "certa" }

    @AfterTest
    fun close() = db.close()

    @Test
    fun RF18_catalogFoldersBooksAndNextPage() =
        runTest {
            val root = source.feed(null)
            root.title shouldBe "Komga"
            root.canSearch shouldBe true
            root.entries.map { it.title } shouldContainExactly listOf("Séries", "Saga & Cia")
            root.entries.none { it.isBook } shouldBe true
            root.entries[0].url shouldBe "${Komga.BASE}/opds/v1.2/series"
            val series = source.feed(root.entries[1].url)
            series.entries.map { it.isBook } shouldContainExactly listOf(true, true)
            series.entries[0].coverUrl shouldBe "${Komga.BASE}/opds/v1.2/books/1/thumbnail"
            series.nextUrl shouldBe "${Komga.BASE}/opds/v1.2/series/saga?page=1"
        }

    @Test
    fun RF18_seriesIsAWorkAndBooksAreChaptersAcrossPages() =
        runTest {
            val work = source.work("${Komga.BASE}/opds/v1.2/series/saga", "Saga")
            val chapters = source.chapters(work)
            chapters.map { it.title } shouldContainExactly listOf("Saga 01", "Saga 02", "Saga 03")
            chapters[0].pageCount shouldBe 3
            source.details(work).authors shouldBe listOf("Brian K. Vaughan")
        }

    @Test
    fun RF18_pagesByStreamWithoutDownloadingTheFile() =
        runTest {
            val work = source.work("${Komga.BASE}/opds/v1.2/series/saga", "Saga")
            val pages = source.pages(work, source.chapters(work)[0])
            pages.map { it.imageUrl } shouldContainExactly
                (0..2).map { "${Komga.BASE}/api/v1/books/1/pages/$it?zero_based=true&width=1600" }
            source.image(pages[1].imageUrl, emptyMap()).decodeToString() shouldBe "page-1"
            requests.none { it.url.encodedPath.endsWith("/file") } shouldBe true
        }

    @Test
    fun RF18_withoutStreamTheCbzIsDownloadedOnceAndRead() =
        runTest {
            val work = source.work("${Komga.BASE}/opds/v1.2/series/saga", "Saga")
            val book = source.chapters(work)[2]
            val pages = source.pages(work, book)
            pages.size shouldBe 2
            source.image(pages[1].imageUrl, emptyMap()).decodeToString() shouldBe "img-2"
            source.image(pages[0].imageUrl, emptyMap()).decodeToString() shouldBe "img-1"
            requests.count { it.url.encodedPath.endsWith("/3/file") } shouldBe 1
        }

    @Test
    fun RF18_openSearchDescription() =
        runTest {
            val found = source.searchCatalog("saga 2")
            requests.last().url.toString() shouldBe "${Komga.BASE}/opds/v1.2/search?search=saga%202"
            found.entries.map { it.title } shouldContainExactly listOf("Saga & Cia")
            source
                .search("saga", emptyList(), 1)
                .items
                .single()
                .key shouldBe "${Komga.BASE}/opds/v1.2/series/saga"
        }

    @Test
    fun RF19_eachProblemHasItsCause() =
        runTest {
            servers.test(ServerInput("K", "ftp://komga.local")) shouldBe ServerProblem.INVALID_ADDRESS
            servers.test(ServerInput("K", "https://tiago:x@komga.local/opds")) shouldBe ServerProblem.CREDENTIALS_IN_ADDRESS
            servers.test(ServerInput("K", komga.url, "tiago", "errada")) shouldBe ServerProblem.WRONG_LOGIN
            servers.test(ServerInput("K", "${Komga.BASE}/web")) shouldBe ServerProblem.NOT_OPDS
            routes = { throw java.net.UnknownHostException("komga.local") }
            servers.test(ServerInput("K", komga.url, "tiago", "certa")) shouldBe ServerProblem.UNREACHABLE
            routes = { throw javax.net.ssl.SSLHandshakeException("PKIX path building failed") }
            servers.test(ServerInput("K", komga.url, "tiago", "certa")) shouldBe ServerProblem.CERTIFICATE
        }

    @Test
    fun RF17_RF72_passwordOnlyInTheSecretStoreAndServerIsASource() =
        runTest {
            servers.save(ServerInput("Casa", komga.url, "tiago", "certa")) shouldBe null
            val saved = servers.observe().first().single()
            saved.name shouldBe "Casa"
            secrets.map.values.single() shouldBe "certa"
            db
                .serverDao()
                .list()
                .single()
                .toString() shouldNotContain "certa"
            (registry[saved.id] as OpdsSource).feed(null).title shouldBe "Komga"
            // A new start registers it again from the database.
            val again = SourceRegistry(emptyList())
            OpdsServers(db, secrets, again, http).start()
            again[saved.id]?.name shouldBe "Casa"
            // Editing without typing the password keeps the saved one.
            servers.save(ServerInput("Casa 2", komga.url, "tiago", ""), saved.id) shouldBe null
            secrets.map.values.single() shouldBe "certa"
            servers
                .observe()
                .first()
                .single()
                .name shouldBe "Casa 2"
            servers.delete(saved.id)
            secrets.map.isEmpty() shouldBe true
            registry[saved.id] shouldBe null
        }

    @Test
    fun RF95_loginNeverFollowsARedirectToAnotherHost() =
        runTest {
            routes = { r ->
                when (r.url.host) {
                    "komga.local" -> Triple(HttpStatusCode.Found, "", mapOf(HttpHeaders.Location to "https://cdn.other/opds"))
                    else -> Triple(HttpStatusCode.OK, Komga.ROOT, emptyMap())
                }
            }
            source.feed(null).title shouldBe "Komga"
            requests.first().headers[HttpHeaders.Authorization] shouldBe OpdsSource.basic("tiago", "certa")
            requests.last().url.host shouldBe "cdn.other"
            requests.last().headers[HttpHeaders.Authorization] shouldBe null
        }

    @Test
    fun RF19_wrongLoginIsAuthOnTheSource() =
        runTest {
            val wrong = OpdsSource(komga, http) { "errada" }
            shouldThrow<SourceException> { wrong.feed(null) }.kind shouldBe SourceException.Kind.AUTH
        }

    @Test
    fun RNF26_xmlEntitiesAndCdata() {
        val node =
            Xml.parse(
                """<?xml version="1.0"?><!DOCTYPE x><a k='1 &amp; 2'><!-- c -->""" +
                    """<b>&lt;x&gt; &#233;&#x1F600;</b><c><![CDATA[<i>]]></c><d/></a>""",
            )
        node.attr("k") shouldBe "1 & 2"
        node.textOf("b") shouldBe "<x> é😀"
        node.textOf("c") shouldBe "<i>"
        node.children.map { it.name } shouldContainExactly listOf("b", "c", "d")
        OpdsFeed.resolve("https://h:8443/a/b/feed", "x.xml") shouldBe "https://h:8443/a/b/x.xml"
        OpdsFeed.resolve("https://h/a/b", "//cdn/x") shouldBe "https://cdn/x"
    }
}

/** A fake Komga: the feeds it answers and the login it wants ("tiago" / "certa"). */
private object Komga {
    const val BASE = "https://komga.local"
    private const val NAV = "application/atom+xml;profile=opds-catalog;kind=navigation"
    private const val ACQ = "application/atom+xml;profile=opds-catalog;kind=acquisition"

    val ROOT = """<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:pse="http://vaemendis.net/opds-pse/ns">
  <title>Komga</title>
  <link rel="search" type="application/opensearchdescription+xml" href="/opds/v1.2/search"/>
  <entry><id>series</id><title>Séries</title><link rel="subsection" type="$NAV" href="/opds/v1.2/series"/></entry>
  <entry><id>saga</id><title>Saga &amp; Cia</title><content>Space opera</content>
    <link rel="subsection" type="$ACQ" href="/opds/v1.2/series/saga"/></entry>
</feed>"""

    private fun book(
        id: Int,
        stream: Boolean,
    ) = """<entry><id>b$id</id><title>Saga 0$id</title><author><name>Brian K. Vaughan</name></author>
    <link rel="http://opds-spec.org/image/thumbnail" type="image/jpeg" href="/opds/v1.2/books/$id/thumbnail"/>
    <link rel="http://opds-spec.org/acquisition" type="application/zip" href="/opds/v1.2/books/$id/file"/>
    ${if (stream) """<link rel="http://vaemendis.net/opds-pse/stream" type="image/jpeg" pse:count="3" href="/api/v1/books/$id/pages/{pageNumber}?zero_based=true&amp;width={maxWidth}"/>""" else ""}
  </entry>"""

    private val SERIES =
        """<feed xmlns="http://www.w3.org/2005/Atom" xmlns:pse="http://vaemendis.net/opds-pse/ns"><title>Saga</title>
  <link rel="next" type="$ACQ" href="/opds/v1.2/series/saga?page=1"/>${book(1, true)}${book(2, true)}</feed>"""
    private val SERIES_2 = """<feed xmlns="http://www.w3.org/2005/Atom"><title>Saga</title>${book(3, false)}</feed>"""
    private const val SEARCH_DESCRIPTION =
        """<OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
  <Url type="application/atom+xml;profile=opds-catalog;kind=acquisition" template="/opds/v1.2/search?search={searchTerms}"/>
</OpenSearchDescription>"""

    private fun cbz(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            listOf("002.jpg" to "img-2", "001.jpg" to "img-1", "ComicInfo.xml" to "<x/>").forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Suppress("ReturnCount")
    fun answer(
        url: String,
        auth: String?,
    ): Triple<HttpStatusCode, Any, Map<String, String>> {
        val path = url.removePrefix(BASE)
        // The web page answers without login; the catalog asks for it.
        if (path == "/web") return Triple(HttpStatusCode.OK, "<!doctype html><html><body>Komga</body></html>", emptyMap())
        if (auth != OpdsSource.basic("tiago", "certa")) return Triple(HttpStatusCode.Unauthorized, "", emptyMap())
        val body: Any =
            when {
                path == "/opds/v1.2/catalog" -> ROOT
                path == "/opds/v1.2/series/saga" -> SERIES
                path == "/opds/v1.2/series/saga?page=1" -> SERIES_2
                path == "/opds/v1.2/search" -> SEARCH_DESCRIPTION
                path.startsWith("/opds/v1.2/search?search=") -> ROOT.replace(Regex("<entry><id>series</id>.*?</entry>"), "")
                path.startsWith("/api/v1/books/1/pages/") -> "page-" + path.substringAfter("/pages/").substringBefore('?')
                path == "/opds/v1.2/books/3/file" -> cbz()
                else -> return Triple(HttpStatusCode.NotFound, "", emptyMap())
            }
        return Triple(HttpStatusCode.OK, body, emptyMap())
    }
}
