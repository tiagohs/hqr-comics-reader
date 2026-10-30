package com.tiagohs.hqr.sources.mangadex

import com.tiagohs.hqr.data.net.hqrHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opt-in check against the real MangaDex (`-PliveCheck=true`): popular, details, chapters, pages and one image.
 * Skipped in the normal build, which never touches the network.
 */
class FEAT001LiveTest {
    @Test
    fun FEAT001_realMangaDexFlow() {
        if (System.getProperty("liveCheck") != "true") return
        runBlocking {
            val source = MangaDexSource(hqrHttpClient("HQR/1.0.0 (https://github.com/tiagohs/hqr-comics-reader)"))
            val popular = source.popular(1)
            assertTrue(popular.items.isNotEmpty(), "popular is empty")
            println("popular: " + popular.items.take(5).map { it.title })
            val work = source.details(popular.items.first())
            println("work: ${work.title} · ${work.authors} · ${work.status} · ${work.genres.take(4)} · cover ${work.coverUrl}")
            val chapters = source.chapters(work).filter { it.externalUrl == null }
            assertTrue(chapters.isNotEmpty(), "no readable chapter")
            println("chapters: ${chapters.size}, first ${chapters.first().number} by ${chapters.first().group}")
            val pages = source.pages(work, chapters.last())
            println("pages: ${pages.size}")
            val bytes = source.image(pages.first().imageUrl, emptyMap())
            println("first page: ${bytes.size} bytes")
            assertTrue(bytes.size > 1000)
            val cover = source.image(work.coverUrl!!, emptyMap())
            println("cover: ${cover.size} bytes")
        }
    }
}
