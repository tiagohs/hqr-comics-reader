// The fixtures are the source's answers as they come, one line each.
@file:Suppress("MaxLineLength")

package com.tiagohs.hqr.sources.archive

import com.tiagohs.hqr.domain.model.SourceManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** Internet Archive over the real answers, trimmed (RF-99). */
class InternetArchiveSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val source =
        InternetArchiveSource(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    val path = request.url.encodedPath
                    val body =
                        when {
                            path == "/advancedsearch.php" -> SEARCH
                            path.startsWith("/metadata/") -> METADATA
                            path.endsWith("/manifest.json") -> MANIFEST
                            else -> return@MockEngine respond("", HttpStatusCode.NotFound)
                        }
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
            sleep = {},
        )

    @Test
    fun RF99_searchAsksOnlyPublicDomainComicsAndTheFirstOpensInTheReader() =
        runTest {
            val page = source.search("Superman", emptyList(), 1)
            val q = requests.single().url.parameters["q"]!!
            q shouldContain "licenseurl:*publicdomain*"
            q shouldContain "collection:comics"
            q shouldContain "AND (Superman)"
            page.hasNextPage shouldBe true
            val first = page.items.first()
            first.title shouldBe "Little Nemo 1905-1914 by Winsor McCay"
            first.authors shouldContainExactly listOf("Winsor McCay")
            first.coverUrl shouldBe "https://archive.org/services/img/LittleNemo1905-1914ByWinsorMccay"
            // A list or a single text: both read.
            first.genres shouldContainExactly listOf("comics", "comic strips", "dreams", "nightmares")
            val manga = SourceManga(InternetArchiveSource.ID, "ComicsNovel01AnarchoDictatorOfDeath1947", "x")
            val details = source.details(manga)
            details.title shouldBe "ANARCHO, DICTATOR OF DEATH -  Comics Novel #1, 1947"
            details.authors shouldContainExactly listOf("Fawcett Publications, Inc.")
            val chapter = source.chapters(details).single()
            val pages = source.pages(details, chapter)
            pages.size shouldBe 2
            pages[0].imageUrl shouldBe
                "https://iiif.archive.org/image/iiif/3/ComicsNovel01AnarchoDictatorOfDeath1947%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_jp2.zip%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_jp2%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_0000.jp2/full/max/0/default.jpg"
        }

    @Test
    fun RF99_querySymbolsNeverReachTheSearchSyntax() =
        runTest {
            source.search("x) OR licenseurl:*", emptyList(), 1)
            requests.single().url.parameters["q"]!! shouldContain "AND (x  or licenseurl)"
        }

    private companion object {
        const val SEARCH =
            """{"responseHeader":{"status":0,"QTime":102},"response":{"numFound":1969,"start":0,"docs":[{"identifier":"LittleNemo1905-1914ByWinsorMccay","title":"Little Nemo 1905-1914 by Winsor McCay","creator":"Winsor McCay","description":"The complete first run of Little Nemo, 1905-1914. Scanned for comicstriplibrary.","downloads":63174,"licenseurl":"http://creativecommons.org/publicdomain/mark/1.0/","publicdate":"2011-03-18T22:07:02Z","subject":["comics","comic strips","dreams","nightmares"]},{"identifier":"ComicsNovel01AnarchoDictatorOfDeath1947","title":"ANARCHO, DICTATOR OF DEATH -  Comics Novel #1, 1947","creator":"Fawcett Publications, Inc.","date":"1947-01-01T00:00:00Z","downloads":2593,"language":"eng","licenseurl":"http://creativecommons.org/publicdomain/mark/1.0/","publicdate":"2015-05-26T05:35:54Z","subject":["vintage comics","comics"]}]}}"""
        const val METADATA =
            """{"server":"ia801601.us.archive.org","dir":"/1/items/ComicsNovel01AnarchoDictatorOfDeath1947","files_count":21,"metadata":{"identifier":"ComicsNovel01AnarchoDictatorOfDeath1947","mediatype":"texts","collection":["comics_inbox","comics","folkscanomy"],"creator":"Fawcett Publications, Inc.","date":"1947","title":"ANARCHO, DICTATOR OF DEATH -  Comics Novel #1, 1947","language":"eng","licenseurl":"http://creativecommons.org/publicdomain/mark/1.0/","subject":["vintage comics","comics"],"publicdate":"2015-05-26 05:35:54"},"files":[{"name":"Comics_Novel_01_Anarcho_Dictator_of_Death_1947.cbz","source":"original","format":"Comic Book ZIP","size":"56767082"}]}"""
        const val MANIFEST =
            """{"type":"Manifest","label":{"none":["ANARCHO..."]},"items":[{"type":"Canvas","width":1382,"height":2045,"label":{"none":["0"]},"items":[{"type":"AnnotationPage","items":[{"type":"Annotation","body":{"type":"Image","format":"image/jpeg","id":"https://iiif.archive.org/image/iiif/3/ComicsNovel01AnarchoDictatorOfDeath1947%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_jp2.zip%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_jp2%2FComics_Novel_01_Anarcho_Dictator_of_Death_1947_0000.jp2/full/max/0/default.jpg"}}]}]},{"type":"Canvas","width":1382,"height":2045,"items":[{"type":"AnnotationPage","items":[{"type":"Annotation","body":{"type":"Image","format":"image/jpeg","id":"https://iiif.archive.org/image/iiif/3/ComicsNovel01AnarchoDictatorOfDeath1947%2Fx_0001.jp2/full/max/0/default.jpg"}}]}]}]}"""
    }
}
