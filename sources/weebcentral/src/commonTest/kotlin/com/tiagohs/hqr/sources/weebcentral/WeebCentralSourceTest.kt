// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.weebcentral

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
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

/** Weeb Central over the site's real markup, trimmed (RF-103, RF-106). */
class WeebCentralSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val times = mutableListOf<Long>()
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private var challenge = false
    private val source =
        WeebCentralSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    times += now.toEpochMilliseconds()
                    if (challenge) {
                        return@MockEngine respond(
                            "<title>Just a moment...</title>",
                            HttpStatusCode.Forbidden,
                            headersOf("cf-mitigated", "challenge"),
                        )
                    }
                    val path = request.url.encodedPath
                    val body =
                        when {
                            path == "/search/data" -> SEARCH
                            path.endsWith("/full-chapter-list") -> CHAPTERS
                            path.endsWith("/images") -> IMAGES
                            path.startsWith("/series/") -> SERIES
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
                },
            ),
            clock = clock,
            sleep = { now += it },
        )

    @Test
    fun RF103_searchFindsTheWorkAndItsLastChapterOpens() =
        runTest {
            val found = source.search("one piece", emptyList(), 1)
            requests.single().url.parameters["text"] shouldBe "one piece"
            found.hasNextPage shouldBe true
            val manga = found.items.single()
            manga.key shouldBe "01J76XY7E9FNDZ1DBBM6PBJPFK"
            manga.title shouldBe "One Piece"
            manga.coverUrl shouldBe "https://temp.compsci88.com/cover/normal/01J76XY7E9FNDZ1DBBM6PBJPFK.webp"
            val details = source.details(manga)
            details.authors shouldContainExactly listOf("ODA Eiichiro")
            details.genres shouldContainExactly listOf("Action", "Shounen")
            details.status shouldBe PublicationStatus.ONGOING
            details.description shouldBe "As a child, Monkey D. Luffy dreamed of becoming the King of the Pirates."
            val chapters = source.chapters(manga)
            chapters.map { it.number } shouldContainExactly listOf(1197.0, 1.0)
            chapters.map { it.key } shouldContainExactly listOf("01M64M8H5WP6CF79EKV0NTH2BD", "01J76XYYR7VK2XCSHBZT8BZ4C2")
            chapters[0].title shouldBe "Chapter 1197"
            chapters[1].group shouldBe "Official"
            chapters[0].uploadedAtEpochMs shouldBe 1_793_304_315_069
            val pages = source.pages(manga, chapters[0])
            pages.map { it.imageUrl } shouldContainExactly
                listOf("https://hot.planeptune.us/manga/One-Piece/0001-001.png", "https://hot.planeptune.us/manga/One-Piece/0001-002.png")
            // RF-103: one request every 2 seconds.
            times.zipWithNext { a, b -> b - a }.all { it >= 2_000 } shouldBe true
        }

    @Test
    fun RF103_chapterNumbersFromTheLabelAndAPlaceholderWithout() {
        WeebCentralSource.chapterNumber("Vol. 2 Chapter 5") shouldBe 5.0
        WeebCentralSource.chapterNumber("Episode 12.5") shouldBe 12.5
        WeebCentralSource.chapterNumber("Season 2 - 7") shouldBe 7.0
        WeebCentralSource.chapterNumber("Oneshot") shouldBe WeebCentralSource.NO_NUMBER
    }

    @Test
    fun RF106_theCloudflareChallengeIsItsOwnError() =
        runTest {
            challenge = true
            shouldThrow<SourceException> { source.popular(1) }.kind shouldBe SourceException.Kind.CHALLENGE
        }

    private companion object {
        const val SEARCH =
            """<article class="bg-base-300 flex gap-4 p-4">
  <section class="w-full lg:w-[25%] xl:w-[20%]">
    <a href="https://weebcentral.com/series/01J76XY7E9FNDZ1DBBM6PBJPFK/One-Piece">
      <article class="lg:hidden relative overflow-hidden">
        <div>
          <div class="w-full aspect-4/6 overflow-hidden">
            <picture>
              <source srcset="https://temp.compsci88.com/cover/small/01J76XY7E9FNDZ1DBBM6PBJPFK.webp" type="image/webp">
              <img src="https://temp.compsci88.com/cover/fallback/01J76XY7E9FNDZ1DBBM6PBJPFK.jpg" alt="One Piece cover">
            </picture>
          </div>
          <div class="w-full h-16 absolute bottom-0 flex"><div class="text-ellipsis truncate text-white">One Piece</div></div>
        </div>
      </article>
    </a>
  </section>
  <section class="hidden lg:block lg:w-[75%] xl:w-[80%]">
    <a href="https://weebcentral.com/series/01J76XY7E9FNDZ1DBBM6PBJPFK/One-Piece"><span>One Piece</span></a>
    <div><strong class="opacity-70">Author(s): </strong><span><a href="https://weebcentral.com/search?author=ODA+Eiichiro">ODA Eiichiro</a></span></div>
  </section>
</article>
<button class="btn" hx-get="/search/data?limit=32&amp;offset=32&amp;sort=Popularity" hx-swap="outerHTML"><span>View More Results...</span></button>"""
        const val SERIES =
            """<html><body><section x-data="{ show_detail: false }" class="flex">
<section class="md:w-4/12 flex flex-col gap-4">
  <section class="flex items-center justify-center"><picture>
    <source srcset="https://temp.compsci88.com/cover/normal/01J76XY7E9FNDZ1DBBM6PBJPFK.webp" type="image/webp">
    <img src="https://temp.compsci88.com/cover/fallback/01J76XY7E9FNDZ1DBBM6PBJPFK.jpg" alt="One Piece cover"></picture></section>
  <section><ul class="flex flex-col gap-4">
    <li><strong>Author(s): </strong><span><a href="https://weebcentral.com/search?author=ODA+Eiichiro">ODA Eiichiro</a></span></li>
    <li><strong>Tags(s): </strong><span><a href="https://weebcentral.com/search?included_tag=Action">Action</a>,</span><span><a href="https://weebcentral.com/search?included_tag=Shounen">Shounen</a></span></li>
    <li><strong>Type: </strong><a href="https://weebcentral.com/search?included_type=Manga">Manga</a></li>
    <li><strong>Status: </strong><a href="https://weebcentral.com/search?included_status=Ongoing">Ongoing</a></li>
  </ul></section>
</section>
<section class="md:w-8/12 flex flex-col gap-4">
  <h1 class="hidden md:block text-2xl font-bold">One Piece</h1>
  <section><ul class="flex flex-col gap-4">
    <li><strong>Description</strong><p class="whitespace-pre-wrap break-words">As a child, Monkey D. Luffy dreamed of becoming the King of the Pirates.</p></li>
  </ul></section>
</section>
</section></body></html>"""
        const val CHAPTERS =
            """<div class="flex items-center" x-data="{ new_chapter: checkNewChapter('2026-10-29T20:05:15.069022Z') }">
  <a href="/chapters/01M64M8H5WP6CF79EKV0NTH2BD" class="hover:bg-base-300 flex-1 flex items-center p-2">
    <span class="me-2"><img src="/static/images/chapter-badge.svg" alt=""></span>
    <span class="grow flex items-center gap-2"><span class="">Chapter 1197</span>
      <span class="flex gap-1 items-center link-info"><span class="hidden md:inline">Last Read</span></span></span>
    <time class="text-datetime opacity-50" datetime="2026-10-29T20:05:15.069Z">2026-10-29T20:05:15.069022Z</time>
  </a>
</div>
<div class="flex items-center" x-data="{ new_chapter: checkNewChapter('2024-09-07T17:04:15.717343Z') }">
  <a href="/chapters/01J76XYYR7VK2XCSHBZT8BZ4C2" class="hover:bg-base-300 flex-1 flex items-center p-2">
    <span class="me-2"><img src="/static/images/chapter-badge-official.svg" alt=""></span>
    <span class="grow flex items-center gap-2"><span class="">Chapter 1</span></span>
    <time class="text-datetime opacity-50" datetime="2024-09-07T17:04:15.717Z">2024-09-07T17:04:15.717343Z</time>
  </a>
</div>"""
        const val IMAGES =
            """<section id="chapter-images" class="w-full flex-1 flex flex-col" x-data="{ scrollDown() {}, scrollUp() {} }">
  <img src="https://hot.planeptune.us/manga/One-Piece/0001-001.png" class="max-w-full h-auto mx-auto" alt="Page 1" loading="lazy" />
  <img src="https://hot.planeptune.us/manga/One-Piece/0001-002.png" class="max-w-full h-auto mx-auto" alt="Page 2" loading="lazy" />
</section>"""
    }
}
