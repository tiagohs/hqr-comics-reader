// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.namicomi

import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** NamiComi over the API's real answers, trimmed (RF-100). */
class NamiComiSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var gate = GATE
    private val source =
        NamiComiSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    val path = request.url.encodedPath
                    val body =
                        when {
                            path == "/title/search" -> LIST
                            path.startsWith(
                                "/title/",
                            ) -> """{"result":"ok","type":"entity","data":${LIST.substringAfter(
                                "\"data\":[",
                            ).substringBeforeLast("],\"meta\"")}}"""
                            path == "/chapter" -> CHAPTERS
                            path == "/gating/check" -> gate
                            path.startsWith("/images/chapter/") -> IMAGES
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
            contentLanguages = { setOf("pt-br") },
            sleep = {},
        )

    @Test
    fun RF100_latestInPortugueseSafeOnlyWithTheLocalCover() =
        runTest {
            val page = source.latest(1)
            val url = requests.single().url
            url.parameters.getAll("availableTranslatedLanguages[]") shouldBe listOf("pt-br")
            url.parameters.getAll("contentRatings[]") shouldBe listOf("safe")
            url.parameters["order[publishedAt]"] shouldBe "desc"
            val work = page.items.single()
            work.title shouldBe "Me apaixonando pelo avô de meu ex-noivo"
            work.coverUrl shouldBe "https://uploads.namicomi.com/covers/73hLs8EN/cfe74395-684d-4435-ac82-3e35d344809b.png.512.jpg"
            work.authors shouldContainExactly listOf("Hanza Art")
            work.genres shouldContainExactly listOf("Comedy")
            work.kind shouldBe ReadingKind.MANHWA
            page.hasNextPage shouldBe true
        }

    @Test
    fun RF100_paidChaptersAreLeftOutAndPagesComeFromTheImageServer() =
        runTest {
            val manga = SourceManga(NamiComiSource.ID, "73hLs8EN", "x")
            source.details(manga).description shouldBe
                "[Age Gap - Comedy] Lady Mary Menoushire was a 20 year-old who just got rejected by her fiance."
            val chapters = source.chapters(manga)
            chapters.map { it.key } shouldContainExactly listOf("BnkaiqRM")
            chapters.single().number shouldBe 3.0
            chapters.single().language shouldBe "pt-br"
            (requests.first { it.method == HttpMethod.Post }.body as TextContent).text shouldContain "\"entityId\":\"PAIDchap\""
            val pages = source.pages(manga, chapters.single())
            pages.single().imageUrl shouldBe
                "https://uploads.namicomi.com/chapter/BnkaiqRM/9f07be46c940b00dcd3ad15659259f3d/high/1-3ecf7c7c12e3b552046e4af19d280650c919ed859e665759100f419dce023b89.png"
        }

    @Test
    fun RF100_withoutTheGateEveryChapterStays() =
        runTest {
            gate = "{}"
            source.chapters(SourceManga(NamiComiSource.ID, "73hLs8EN", "x")).size shouldBe 2
        }

    private companion object {
        const val GATE =
            """{"result":"ok","type":"entity","data":{"id":"x","type":"entity_access_map","attributes":{"map":{"BnkaiqRM":true,"PAIDchap":false}},"relationships":[]}}"""
        const val LIST =
            """{"result":"ok","type":"collection","data":[{"id":"73hLs8EN","type":"title","attributes":{"slug":"falling-in-love-with-my-ex-fiance-s-grandfather","title":{"en":"Falling in Love With My Ex-Fiance's Grandfather","pt-br":"Me apaixonando pelo avô de meu ex-noivo"},"description":{"en":"[Age Gap - Comedy] Lady Mary Menoushire was a 20 year-old who just got rejected by her fiance."},"originalLanguage":"en","year":2024,"type":"manwha","contentRating":"safe","publicationStatus":"ongoing","readingMode":"vls","publishedAt":"2024-12-01T09:17:55+00:00"},"relationships":[{"id":"880de87d-c8c5-412a-b53a-74c28fd0e545","type":"creator"},{"id":"WMQb26Jh","type":"organization","attributes":{"slug":"hanzaart","name":"Hanza Art"}},{"id":"JWbT8Fwh","type":"cover_art","attributes":{"description":"","volume":null,"fileName":"cfe74395-684d-4435-ac82-3e35d344809b.png","locale":"pt-br"}},{"id":"PHtTvi6X","type":"cover_art","attributes":{"description":"","volume":null,"fileName":"2efdb6a0-1da7-467b-91bf-e9629b53a03c.jpg","locale":"en"}},{"id":"comedy","type":"primary_tag","attributes":{"slug":"comedy","name":{"en":"Comedy"},"group":"genre"}}]}],"meta":{"limit":20,"offset":0,"total":89}}"""
        const val CHAPTERS =
            """{"result":"ok","type":"collection","data":[{"id":"BnkaiqRM","type":"chapter","attributes":{"volume":null,"chapter":"3","name":null,"translatedLanguage":"pt-br","state":"published","publishAt":"2025-03-27T20:00:00+00:00","createdAt":"2025-03-06T16:26:15+00:00","pages":20},"relationships":[{"id":"73hLs8EN","type":"title"},{"id":"WMQb26Jh","type":"organization"}]},{"id":"PAIDchap","type":"chapter","attributes":{"volume":null,"chapter":"4","name":"Pago","translatedLanguage":"pt-br","publishAt":"2025-04-03T20:00:00+00:00","pages":20},"relationships":[]}],"meta":{"limit":200,"offset":0,"total":2}}"""
        const val IMAGES =
            """{"result":"ok","type":"image_data","data":{"baseUrl":"https://uploads.namicomi.com","hash":"9f07be46c940b00dcd3ad15659259f3d","source":[{"size":23322,"filename":"1-3ecf7c7c12e3b552046e4af19d280650c919ed859e665759100f419dce023b89.png","resolution":"2050x773"}],"high":[{"size":23322,"filename":"1-3ecf7c7c12e3b552046e4af19d280650c919ed859e665759100f419dce023b89.png","resolution":"2050x773"}],"medium":[],"low":[]}}"""
    }
}
