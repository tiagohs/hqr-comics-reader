package com.tiagohs.hqr.sources.hqnow

import com.tiagohs.hqr.data.net.hqrHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opt-in check against the real HQ Now (`-PliveCheck=true`): popular, latest, search, work, chapters, pages and one
 * image with its Referer. Skipped in the normal build, which never touches the network.
 */
class FEAT013LiveTest {
    @Test
    fun FEAT013_realHqNowFlow() {
        if (System.getProperty("liveCheck") != "true") return
        runBlocking {
            val http = hqrHttpClient("HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)")
            val source = HqNowSource(http)
            val popular = source.popular(1)
            assertTrue(popular.items.isNotEmpty(), "popular is empty")
            println("popular: " + popular.items.take(5).map { it.title })
            println(
                "latest: " +
                    source
                        .latest(1)
                        .items
                        .take(5)
                        .map { it.title },
            )
            val found = source.search("batman", emptyList(), 1)
            assertTrue(found.items.isNotEmpty(), "search found nothing")
            val work = source.details(found.items.first())
            println("work: ${work.title} · ${work.status} · cover ${work.coverUrl}")
            val chapters = source.chapters(work)
            assertTrue(chapters.isNotEmpty(), "no chapter")
            println("chapters: ${chapters.size}, first ${chapters.first().number}")
            val pages = source.pages(work, chapters.first())
            assertTrue(pages.isNotEmpty(), "no page")
            val page = pages.first()
            val bytes =
                http.get(page.imageUrl) { page.headers.forEach { (k, v) -> header(k, v) } }.bodyAsBytes()
            println("pages: ${pages.size}, first page ${bytes.size} bytes")
            assertTrue(bytes.size > 1000)
        }
    }
}
