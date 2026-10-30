// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.yomu

import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Yomu Mangás over answers built from the API's shapes (RF-102). The API refused requests from outside Brazil while
 * this was written, so the live check (with `-PliveCheck=true`) is what confirms them.
 */
class YomuSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val times = mutableListOf<Long>()
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private var chapterPage = PAGES
    private val source =
        YomuSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    times += now.toEpochMilliseconds()
                    val path = request.url.encodedPath
                    val body =
                        when {
                            path == "/search/medias" -> SEARCH
                            path == "/home/updates" -> UPDATES
                            path.endsWith("/chapters") -> CHAPTERS
                            path.startsWith("/mangas/123/solo-leveling/") -> chapterPage
                            path.startsWith("/mangas/") -> WORK
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK)
                },
            ),
            clock = clock,
            sleep = { now += it },
        )

    @Test
    fun RF102_aWorkFromMangaFlixIsFoundByTitleAndItsChaptersRead() =
        runTest {
            val found = source.search("Solo Leveling", emptyList(), 1)
            requests.single().url.parameters["q"] shouldBe "Solo Leveling"
            found.hasNextPage shouldBe true
            val manga = found.items.single()
            // The id only: the slug goes in the address, where a change on the site can't make it another work.
            manga.key shouldBe "123"
            manga.url shouldBe "https://yomumangas.com/mangas/123/solo-leveling"
            manga.coverUrl shouldBe "https://b2.yomumangas.com/mangas/123/cover.webp"
            source.details(manga).authors shouldContainExactly listOf("Chugong")
            requests.last().url.encodedPath shouldBe "/mangas/123"
            val chapters = source.chapters(manga)
            // Newest first, the title without the leading dash.
            chapters.map { it.number } shouldContainExactly listOf(1.5, 1.0)
            chapters[1].title shouldBe "O Caçador Mais Fraco"
            val pages = source.pages(manga, chapters[1])
            pages.map { it.imageUrl } shouldContainExactly
                listOf("https://b2.yomumangas.com/chapters/123/1/001.webp", "https://b2.yomumangas.com/chapters/123/1/002.webp")
            requests.last().url.toString() shouldBe "https://yomumangas.com/mangas/123/solo-leveling/1"
            times.zipWithNext { a, b -> b - a }.all { it >= 1_000 } shouldBe true
        }

    @Test
    fun RF102_aChapterPageWithoutImagesIsAnError() =
        runTest {
            chapterPage = "<html></html>"
            shouldThrow<SourceException> {
                source.pages(
                    SourceManga(YomuSource.ID, "123/solo-leveling", "x"),
                    com.tiagohs.hqr.domain.model
                        .SourceChapter("1", 1.0),
                )
            }.kind shouldBe SourceException.Kind.PARSE
            source
                .latest(1)
                .items
                .single()
                .status shouldBe com.tiagohs.hqr.domain.model.PublicationStatus.COMPLETED
        }

    private companion object {
        const val SEARCH =
            """{"mangas":[{"id":123,"slug":"solo-leveling","title":"Solo Leveling","cover":"b2://mangas/123/cover.webp"}],"pages":4}"""
        const val UPDATES =
            """{"medias":[{"id":123,"slug":"solo-leveling","title":"Solo Leveling","cover":"b2://mangas/123/cover.webp","status":"COMPLETE","description":"Dez anos atrás...","authors":["Chugong"],"artists":["DUBU (Redice Studio)"],"genres":[{"name":"Ação"}],"tags":[{"name":"Dungeon"}]}]}"""
        const val WORK =
            """{"manga":{"id":123,"slug":"solo-leveling","title":"Solo Leveling","cover":"b2://mangas/123/cover.webp","status":"ONGOING","description":"Dez anos atrás...","authors":["Chugong"],"artists":[],"genres":[{"name":"Ação"},{"name":"Fantasia"}],"tags":[]}}"""
        const val CHAPTERS =
            """{"chapters":[{"chapter":"1","title":"- O Caçador Mais Fraco","uploaded_at":"2024-03-01T12:00:00.000Z"},{"chapter":"1.5","title":null,"uploaded_at":"2024-03-08T12:00:00Z"}]}"""
        const val PAGES =
            """<script>self.__next_f.push([1,"{\"pages\":[\"b2://chapters/123/1/001.webp\",\"b2://chapters/123/1/002.webp\"]}"])</script>"""
    }
}
