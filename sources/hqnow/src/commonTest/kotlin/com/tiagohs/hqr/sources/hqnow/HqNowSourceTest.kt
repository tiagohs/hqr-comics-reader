package com.tiagohs.hqr.sources.hqnow

import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.source.SourceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

class HqNowSourceTest {
    private val bodies = mutableListOf<String>()
    private var answer: (String) -> Pair<String, HttpStatusCode> = { body -> Fixtures.answer(body) to HttpStatusCode.OK }
    private var now = Instant.fromEpochMilliseconds(0)
    private val clock =
        object : Clock {
            override fun now() = now
        }
    private val source =
        HqNowSource(
            HttpClient(
                MockEngine { request ->
                    val body = (request.body as TextContent).text
                    bodies += body
                    val (text, status) = answer(body)
                    respond(text, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                },
            ),
            clock,
            // The limiter waits on the fake clock (a no-op sleep with a frozen clock would wait forever).
            sleep = { now += it },
        )
    private val work = SourceManga(SourceId.HQ_NOW, "2219", "Invencível (2003)")

    @Test
    fun RF12_popularIsMostViewedInSlicesOf30() =
        runTest {
            val first = source.popular(1)
            first.items.size shouldBe 30
            first.hasNextPage shouldBe true
            bodies.last() shouldContain "limit: 30"
            val second = source.popular(2)
            bodies.last() shouldContain "limit: 60"
            second.items.first().key shouldBe "31"
            source.popular(3).hasNextPage shouldBe false
        }

    @Test
    fun RF12_latestAndSearch() =
        runTest {
            source.latest(1).items.map { it.title } shouldContainExactly listOf("Doutor Estranho (2023)")
            source.latest(2).items shouldBe emptyList()
            source
                .search("Invencível", emptyList(), 1)
                .items
                .single()
                .key shouldBe "2219"
            bodies.last() shouldContain "\"name\":\"Invencível\""
            source.search(" ", emptyList(), 1).items shouldBe emptyList()
        }

    @Test
    fun RF12_workChaptersAndPages() =
        runTest {
            val details = source.details(work)
            details.status shouldBe PublicationStatus.COMPLETED
            details.kind shouldBe ReadingKind.COMIC
            details.genres shouldContainExactly listOf("Image Comics")
            details.coverUrl shouldBe "https://static.hq-now.com/capa.jpg"
            details.url shouldBe "https://www.hq-now.com/hq/2219/invencivel-2003"
            val chapters = source.chapters(work)
            chapters.map { it.number } shouldContainExactly listOf(144.0, 0.1, 0.0)
            chapters[1].title shouldBe "Marvel Team-Up #14"
            chapters[0].title shouldBe null
            val pages = source.pages(work, SourceChapter("24233", 0.0))
            pages.map { it.imageUrl } shouldContainExactly listOf("https://static.hq-now.com/p1.jpg", "https://static.hq-now.com/p2.jpg")
            pages.first().headers["Referer"] shouldBe "https://www.hq-now.com/"
        }

    @Test
    fun RF05_failuresKeepTheirKind() =
        runTest {
            answer = { "{}" to HttpStatusCode.BadGateway }
            shouldThrow<SourceException> { source.popular(1) }.kind shouldBe SourceException.Kind.SERVER
            answer = { """{"errors":[{"message":"x"}]}""" to HttpStatusCode.OK }
            shouldThrow<SourceException> { source.latest(1) }.kind shouldBe SourceException.Kind.SERVER
            answer = { """{"data":{"getHqsById":[]}}""" to HttpStatusCode.OK }
            shouldThrow<SourceException> { source.details(work) }.kind shouldBe SourceException.Kind.NOT_FOUND
            answer = { "<html>" to HttpStatusCode.OK }
            shouldThrow<SourceException> { source.latest(1) }.kind shouldBe SourceException.Kind.PARSE
            shouldThrow<SourceException> { source.details(work.copy(key = "abc")) }.kind shouldBe SourceException.Kind.PARSE
        }

    @Test
    fun RF12_mappings() {
        hqNowStatus("Em andamento") shouldBe PublicationStatus.ONGOING
        hqNowStatus("Cancelado") shouldBe PublicationStatus.CANCELLED
        hqNowStatus("Hiato") shouldBe PublicationStatus.HIATUS
        hqNowStatus(null) shouldBe PublicationStatus.UNKNOWN
        slug("Ação & Reação: Volume 2!") shouldBe "acao-reacao-volume-2"
        secure("https://x/a.jpg") shouldBe "https://x/a.jpg"
    }
}

/** Answers shaped like the HQ Now GraphQL API, by the field the query asks for. */
private object Fixtures {
    private fun hq(id: Int) =
        """{"id":$id,"name":"Obra $id","editoraId":5,"status":"Concluído",""" +
            """"hqCover":"http://static.hq-now.com/c$id.jpg","synopsis":"S","updatedAt":"2021"}"""

    // The real schema (checked live, FEAT013LiveTest): UpdatedHqs has no status nor editoraId, and the variables are
    // required (String!, Int!). The fake refuses the same queries the server refuses.
    private val refused =
        listOf(
            Regex("getRecentlyUpdatedHqs \\{[^}]*(status|editoraId)"),
            Regex("\\$(name|id): (String|Int)[^!]"),
        )

    fun answer(body: String): String =
        when {
            refused.any { it.containsMatchIn(body) } -> """{"errors":[{"message":"GRAPHQL_VALIDATION_FAILED"}]}"""

            "getHqsByFilters" in body -> {
                val limit = Regex("limit: (\\d+)").find(body)!!.groupValues[1].toInt()
                // 75 works in total: the third page is short.
                val list = (1..minOf(limit, 75)).joinToString(",") { hq(it) }
                """{"data":{"getHqsByFilters":[$list]}}"""
            }
            "getRecentlyUpdatedHqs" in body ->
                """{"data":{"getRecentlyUpdatedHqs":[{"id":9,"name":"Doutor Estranho (2023)","hqCover":null,"synopsis":""}]}}"""
            "getHqsByName" in body -> """{"data":{"getHqsByName":[${hq(2219)}]}}"""
            "getHqsById" in body ->
                """{"data":{"getHqsById":[{"id":2219,"name":"Invencível (2003)","status":"Concluído","publisherName":"Image Comics",
                "hqCover":"http://static.hq-now.com/capa.jpg","synopsis":" Mark Grayson. ","capitulos":[
                {"id":24233,"name":"","number":"0"},{"id":24231,"name":"Marvel Team-Up #14","number":"0.1"},
                {"id":28007,"name":"","number":"144"}]}]}}"""
            "getChapterById" in body ->
                """{"data":{"getChapterById":{"pictures":[{"pictureUrl":"http://static.hq-now.com/p1.jpg"},""" +
                    """{"pictureUrl":"http://static.hq-now.com/p2.jpg"}]}}}"""
            else -> """{"errors":[{"message":"unknown"}]}"""
        }
}
