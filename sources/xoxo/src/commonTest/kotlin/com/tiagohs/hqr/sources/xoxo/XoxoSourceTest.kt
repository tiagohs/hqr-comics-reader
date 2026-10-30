// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.xoxo

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/** XOXO Comics over the site's real markup, trimmed (RF-104). */
class XoxoSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val times = mutableListOf<Long>()
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val source =
        XoxoSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    times += now.toEpochMilliseconds()
                    val path = request.url.encodedPath
                    val body =
                        when {
                            path == "/hot-comic" || path == "/search-comic" -> POPULAR
                            path == "/comic-update" -> LATEST
                            path.endsWith("/all") -> PAGES
                            path == "/comic/nightwing-2016" && request.url.parameters["page"] == "2" -> SECOND_PAGE
                            path == "/comic/nightwing-2016" -> DETAIL
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
                },
            ),
            clock = clock,
            sleep = { now += it },
        )

    @Test
    fun RF104_latestIssuesWithCoverAndNumber() =
        runTest {
            val latest = source.latest(1)
            latest.hasNextPage shouldBe true
            val work = latest.items.single()
            work.key shouldBe "nightwing-2016"
            work.title shouldBe "Nightwing (2016)"
            work.coverUrl shouldBe
                "https://1.bp.blogspot.com/-U-utw_1UNdQ/XYIX_02f8NI/AAAAAAAAQoE/X2rmW8sGzrM9Kb510j5zs0V1HUxdo66oQCLcBGAsYHQ/s1600/optimized-olnw.jpg"
            source
                .popular(1)
                .items
                .single()
                .coverUrl shouldBe work.coverUrl
            source.search("night wing", emptyList(), 1)
            requests.last().url.parameters["keyword"] shouldBe "night wing"
            times.zipWithNext { a, b -> b - a }.all { it >= 1_000 } shouldBe true
        }

    @Test
    fun RF104_detailsEveryChapterPageAndTheIssuePages() =
        runTest {
            val manga = SourceManga(XoxoSource.ID, "nightwing-2016", "x")
            val details = source.details(manga)
            details.title shouldBe "Nightwing (2016)"
            details.authors shouldContainExactly listOf("Tim Seeley")
            details.genres shouldContainExactly listOf("DC Comics", "Action")
            details.status shouldBe PublicationStatus.ONGOING
            val chapters = source.chapters(manga)
            chapters.map { it.number } shouldContainExactly listOf(143.0, 142.0, 1.0)
            chapters.map { it.key } shouldContainExactly listOf("issue-143", "issue-142", "issue-1")
            chapters[0].uploadedAtEpochMs shouldBe 1_792_627_200_000
            val pages = source.pages(manga, chapters[0])
            pages.map { it.imageUrl.substringAfterLast("/") } shouldContainExactly listOf("1.jpg", "2.jpg")
            requests.last().url.encodedPath shouldBe "/comic/nightwing-2016/issue-143/all"
        }

    @Test
    fun RF104_annualsDoNotTakeTheIssueNumbers() {
        XoxoSource.issueNumber("Nightwing (2016) Issue #143") shouldBe 143.0
        XoxoSource.issueNumber("Nightwing (2016) Annual #1") shouldBe XoxoSource.NO_NUMBER
        XoxoSource.issueNumber("Nightwing (2016) TPB") shouldBe XoxoSource.NO_NUMBER
    }

    private companion object {
        const val POPULAR =
            """<div class="items"><div class="row"><div class="item"><figure class="clearfix">
  <div class="image"><a href="https://xoxocomic.com/comic/nightwing-2016" title="Nightwing (2016) Comic">
    <img class="lazy" alt="Nightwing (2016)" data-original="https://1.bp.blogspot.com/-U-utw_1UNdQ/XYIX_02f8NI/AAAAAAAAQoE/X2rmW8sGzrM9Kb510j5zs0V1HUxdo66oQCLcBGAsYHQ/s1600/optimized-olnw.jpg" src="data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="></a></div>
  <figcaption><h3><a class="jtip" href="https://xoxocomic.com/comic/nightwing-2016">Nightwing (2016)</a></h3>
    <p class="chapter">Latest: <a href="https://xoxocomic.com/comic/nightwing-2016/issue-143">Issue #143</a></p></figcaption>
</figure></div></div></div>
<ul class="pagination"><li class="active"><span>1</span></li><li><a href="https://xoxocomic.com/hot-comic?page=2" rel="next">»</a></li></ul>"""
        const val LATEST =
            """<ul><li class="row">
  <div class="col-xs-9"><h3><a class="jtip" href="https://xoxocomic.com/comic/nightwing-2016">Nightwing (2016)</a></h3>
    <ul class="hlb-list"><li><a href="https://xoxocomic.com/comic/nightwing-2016/issue-143">Nightwing (2016) Issue #143</a></li></ul></div>
  <div class="col-xs-3 text-center">10/22/2026</div>
  <div class="box_tootip" style="display:none;"><div class="box_img"><a href="https://xoxocomic.com/comic/nightwing-2016">
    <img class="lazy" data-original="https://1.bp.blogspot.com/-U-utw_1UNdQ/XYIX_02f8NI/AAAAAAAAQoE/X2rmW8sGzrM9Kb510j5zs0V1HUxdo66oQCLcBGAsYHQ/s1600/optimized-olnw.jpg" src="data:image/gif;base64,R0l="></a></div></div>
</li></ul>
<ul class="pagination"><li><a href="comic-update?page=2" rel="next">»</a></li></ul>"""
        const val DETAIL =
            """<article id="item-detail"><h1 class="title-detail">Nightwing (2016) Comic</h1>
<div class="detail-info"><div class="row"><div class="col-xs-4 col-image"><img alt="Nightwing (2016) cover image" src="https://1.bp.blogspot.com/cover.jpg"></div>
<div class="col-xs-8 col-info"><ul class="list-info">
  <li class="author row"><p class="name col-xs-4"><strong>Author(s)</strong></p><p class="col-xs-8"> Tim Seeley </p></li>
  <li class="status row"><p class="name col-xs-4"><strong>Status</strong></p><p class="col-xs-8">Ongoing</p></li>
  <li class="kind row"><p class="name col-xs-4"><strong>Genres</strong></p><p class="col-xs-8"><a href="https://xoxocomic.com/dc-comics-comic">DC Comics</a> - <a href="https://xoxocomic.com/action-comic">Action</a></p></li>
</ul></div></div></div>
<div class="detail-content"><h3 class="list-title">Summary</h3><p>Hes been Robin, Batman, a spy, a ghost.</p></div>
<div class="list-chapter" id="nt_listchapter"><nav><ul>
  <li class="row heading"><div class="col-xs-9 no-wrap">Issue Name</div><div class="col-xs-3 no-wrap text-center">Day Added</div></li>
  <li class="row"><div class="col-xs-9 chapter"><a href="https://xoxocomic.com/comic/nightwing-2016/issue-143"> Nightwing (2016) Issue #143</a></div><div class="col-xs-3 text-center">10/22/2026</div></li>
  <li class="row"><div class="col-xs-9 chapter"><a href="https://xoxocomic.com/comic/nightwing-2016/issue-142"> Nightwing (2016) Issue #142</a></div><div class="col-xs-3 text-center">10/19/2026</div></li>
</ul></nav></div></article>
<ul class="pagination"><li class="active"><span>1</span></li><li><a href="nightwing-2016?page=2" rel="next">»</a></li></ul>"""
        const val SECOND_PAGE =
            """<article id="item-detail"><div class="list-chapter"><nav><ul>
  <li class="row"><div class="col-xs-9 chapter"><a href="https://xoxocomic.com/comic/nightwing-2016/issue-1"> Nightwing (2016) Issue #1</a></div><div class="col-xs-3 text-center">07/20/2016</div></li>
</ul></nav></div></article>"""
        const val PAGES =
            """<div class="reading-detail box_doc">
  <div class="page-chapter" id="page_1"><img alt="Page 1" class="lazy" data-index="1" data-original="https://xoxocomic.com/comic/nightwing-2016/issue-143/303942/1.jpg" src="data:image/png;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="></div>
  <div class="page-chapter" id="page_2"><img alt="Page 2" class="lazy" data-index="2" data-original="https://xoxocomic.com/comic/nightwing-2016/issue-143/303942/2.jpg" src="data:image/png;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="></div>
  <div class="page-chapter" id="page_3"><img alt="Page 2 again" class="lazy" data-original="https://xoxocomic.com/comic/nightwing-2016/issue-143/303942/2.jpg"></div>
</div>"""
    }
}
