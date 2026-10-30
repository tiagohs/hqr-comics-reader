// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.mangaflix

import com.tiagohs.hqr.domain.model.SourceManga
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

/** MangaFlix over the API's real answers, trimmed (RF-101). */
class MangaFlixSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val times = mutableListOf<Long>()
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private var broken = false
    private val source =
        MangaFlixSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    times += now.toEpochMilliseconds()
                    val path = request.url.encodedPath
                    val body =
                        when {
                            broken -> """{"data":{"nope":1}}"""
                            path.endsWith("/browse") -> BROWSE
                            path.endsWith("/latest-releases") -> LATEST
                            path.endsWith("/search/mangas") -> SEARCH
                            path.contains("/mangas/") -> WORK
                            path.contains("/chapters/") -> IMAGES
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
            clock = clock,
            sleep = { now += it },
        )

    @Test
    fun RF101_popularLatestAndSearchAtOneRequestPerSecond() =
        runTest {
            // The real /browse has a work without a name on another shelf: the list still reads.
            source.popular(1).items.map { it.title } shouldContainExactly listOf("Martial Peak")
            source.popular(2).items shouldBe emptyList()
            val latest = source.latest(1).items.single()
            latest.title shouldBe "Surviving in an RPG Maker Horror Game"
            requests[1].url.parameters["selected_language"] shouldBe "pt-br"
            val found = source.search("one piece", emptyList(), 1).items.single()
            found.key shouldBe "651602a329c6a805b18d594f"
            found.coverUrl shouldBe "https://static.mangaflix.net/up/9655a702-f6ea-466d-9bc5-33dcbccf8e0e/optimized.jpg"
            requests.last().url.parameters["query"] shouldBe "one piece"
            // RF-101: never more than one request in the same second.
            times.zipWithNext { a, b -> b - a }.all { it >= 1_000 } shouldBe true
        }

    @Test
    fun RF101_workChaptersAndPagesInOrder() =
        runTest {
            val manga = SourceManga(MangaFlixSource.ID, "651602a329c6a805b18d594f", "One Piece")
            val details = source.details(manga)
            details.genres shouldContainExactly listOf("Ação")
            details.url shouldBe "https://mangaflix.net/br/manga/651602a329c6a805b18d594f"
            val chapters = source.chapters(manga)
            chapters.map { it.number } shouldContainExactly listOf(1155.0, 0.0)
            chapters[0].title shouldBe null
            chapters[1].title shouldBe "Strong World"
            chapters[0].group shouldBe "ArinVale"
            chapters[0].uploadedAtEpochMs shouldBe 1_754_006_400_000
            val pages = source.pages(manga, chapters[0])
            pages.map { it.imageUrl.substringAfterLast("/chap/") } shouldContainExactly
                listOf("c69c4def-e159-4bab-83d5-1d0a2a4263f4/full.jpeg", "70062fb8-f09c-4ebc-8651-c76c3d230910/full.jpeg")
        }

    @Test
    fun RF05_anAnswerOfAnotherShapeIsAParseError() =
        runTest {
            broken = true
            shouldThrow<SourceException> { source.popular(1) }.kind shouldBe SourceException.Kind.PARSE
        }

    private companion object {
        const val BROWSE =
            """{"data":[{"key":"most-read","items":[{"_id":"6513a5c4d0e2acdd1e1ab741","name":"Martial Peak","description":"A jornada para o pico marcial é solitária...","poster":{"default_url":"https://static.mangaflix.net/data/mangas/6513a5c4d0e2acdd1e1ab741/posters/30c946e2-ab3e-4ca7-84b8-968c834311c4.jpg"},"genres":[{"_id":"6511e95bae08773cd411c21c","name":"Ação","lang":"pt-br","adult":false}]}]},{"key":"trending","items":[]},{"key":"samurai","title":"Samurai","items":[{"_id":"x1","poster":null,"genres":[],"is_adult":false}]}],"status":200}"""
        const val LATEST =
            """{"data":[{"_id":"6a8b52fdf14610a8af7898f7","name":"Surviving in an RPG Maker Horror Game","description":"No instante em que inicia...","poster":{"default_url":"https://static.mangaflix.net/up/ac8e4871-d0c6-495e-bfe9-956e9767f044/optimized.jpg"},"is_adult":false,"chapters":[{"_id":"6ae431bb7c41ebb6e5b95d85","number":"15","created_at":"2026-10-30T05:57:15.540Z"}]}],"metadata":{"total":100},"status":200}"""
        const val SEARCH =
            """{"data":{"works":[{"_id":"651602a329c6a805b18d594f","name":"One Piece","description":"Gol D. Roger...","poster":{"default_url":"https://static.mangaflix.net/up/9655a702-f6ea-466d-9bc5-33dcbccf8e0e/optimized.jpg"},"genres":["6511e95bae08773cd411c21c","6511e95bae08773cd411c224"]}]},"status":200}"""
        const val WORK =
            """{"data":{"_id":"651602a329c6a805b18d594f","name":"One Piece","description":"Gol D. Roger...","poster":{"default_url":"https://static.mangaflix.net/up/9655a702-f6ea-466d-9bc5-33dcbccf8e0e/optimized.jpg"},"genres":[{"_id":"6511e95bae08773cd411c21c","name":"Ação"}],"chapters_count":2,"comments_count":22,"content_type":"manga","chapters":[{"_id":"688d16e31cca2c452a208417","number":"1155","name":"","iso_date":"2025-08-01T00:00:00.000Z","owners":[{"_id":"654467fefb0ffc89ad0a63b2","name":"ArinVale"}],"number_of_pages":16,"pages_integrity_status":"full"},{"_id":"651607a129c6a805b198fb00","number":"0","name":"Strong World","date":"15/07/12","iso_date":"2012-07-15T00:00:00.000Z","owners":[{"_id":"654467fefb0ffc89ad0a63b2","name":"ArinVale"}],"number_of_pages":22}]},"status":200}"""
        const val IMAGES =
            """{"data":{"_id":"688d16e31cca2c452a208417","lang":"pt-br","name":"","number":"1155","iso_date":"2025-08-01T00:00:00.000Z","manga":"651602a329c6a805b18d594f","owners":["654467fefb0ffc89ad0a63b2"],"images_count":2,"images":[{"_id":"688d16e71cca2c452a20851c","default_url":"https://static.mangaflix.net/up/chap/70062fb8-f09c-4ebc-8651-c76c3d230910/full.jpeg","order":2},{"_id":"688d16e5175e9009bf8bde16","default_url":"https://static.mangaflix.net/up/chap/c69c4def-e159-4bab-83d5-1d0a2a4263f4/full.jpeg","order":1}]},"status":200}"""
    }
}
