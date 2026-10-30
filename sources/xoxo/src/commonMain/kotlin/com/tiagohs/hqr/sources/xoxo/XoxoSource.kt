package com.tiagohs.hqr.sources.xoxo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.tiagohs.hqr.data.net.RateLimiter
import com.tiagohs.hqr.data.net.ensureOk
import com.tiagohs.hqr.data.net.networkCall
import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * XOXO Comics (RF-104): Western comics read from the site's pages, one request per second. The work key is the comic's
 * slug ("nightwing-2016"); the chapter's is the issue's path under it ("issue-143").
 */
class XoxoSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "XOXO Comics"
    override val languages: List<String> = listOf("en")

    override fun webUrl(query: String): String = if (query.isBlank()) SITE else "$SITE/search-comic?keyword=$query"

    private val limiter = RateLimiter(1, 1.seconds, clock, sleep)

    private suspend fun page(url: String): Document {
        limiter.acquire()
        val html = networkCall { http.get(url).ensureOk().bodyAsText() }
        return Ksoup.parse(html, url)
    }

    private fun paged(
        path: String,
        page: Int,
        extra: String = "",
    ): String {
        val params = listOfNotNull(extra.ifEmpty { null }, if (page > 1) "page=$page" else null).joinToString("&")
        return if (params.isEmpty()) "$SITE/$path" else "$SITE/$path?$params"
    }

    private fun hasNext(doc: Document) = doc.selectFirst("ul.pagination a[rel=next], a.next-page") != null

    override suspend fun popular(page: Int): MangaPage {
        val doc = page(paged("hot-comic", page))
        return MangaPage(doc.select("div.items div.item").mapNotNull { it.card() }, hasNext(doc))
    }

    override suspend fun latest(page: Int): MangaPage {
        val doc = page(paged("comic-update", page))
        val items =
            doc.select("li.row").mapNotNull { row ->
                val link = row.selectFirst("h3 a") ?: return@mapNotNull null
                manga(link.attr("abs:href"), link.text(), row.selectFirst("img[data-original]")?.lazySrc())
            }
        return MangaPage(items, hasNext(doc))
    }

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        if (query.isBlank()) return popular(page)
        val doc = page(paged("search-comic", page, "keyword=${query.trim().encodeURLParameter()}"))
        return MangaPage(doc.select("div.items div.item").mapNotNull { it.card() }, hasNext(doc))
    }

    private fun Element.card(): SourceManga? {
        val link = selectFirst("h3 a") ?: return null
        return manga(link.attr("abs:href"), link.text(), selectFirst("div.image img")?.lazySrc())
    }

    /** The real address is in "data-original"; "src" holds a placeholder until the page scrolls. */
    private fun Element.lazySrc(): String? =
        listOf("data-original", "data-src", "src")
            .map { attr("abs:$it") }
            .firstOrNull { it.isNotBlank() && !it.startsWith("data:") }

    private fun manga(
        url: String,
        title: String,
        cover: String?,
    ): SourceManga? {
        val slug = slugOf(url) ?: return null
        return SourceManga(
            sourceId = ID,
            key = slug,
            title = title.trim(),
            coverUrl = cover,
            url = "$SITE/comic/$slug",
            kind = ReadingKind.COMIC,
            language = "en",
        )
    }

    override suspend fun details(manga: SourceManga): SourceManga {
        val doc = page("$SITE/comic/${manga.key}")
        val info = doc.selectFirst("article#item-detail") ?: throw SourceException("Página da obra mudou", SourceException.Kind.PARSE)
        return manga.copy(
            title =
                info
                    .selectFirst("h1.title-detail")
                    ?.text()
                    ?.removeSuffix(" Comic")
                    ?.trim()
                    ?.ifEmpty { null } ?: manga.title,
            coverUrl = info.selectFirst("div.col-image img")?.lazySrc() ?: manga.coverUrl,
            url = "$SITE/comic/${manga.key}",
            authors =
                info
                    .selectFirst("li.author p.col-xs-8")
                    ?.text()
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty(),
            genres = info.select("li.kind p.col-xs-8 a").map { it.text().trim() },
            status =
                when (info.selectFirst("li.status p.col-xs-8")?.text()?.trim()) {
                    "Ongoing" -> PublicationStatus.ONGOING
                    "Completed" -> PublicationStatus.COMPLETED
                    else -> PublicationStatus.UNKNOWN
                },
            description = info.select("div.detail-content p").joinToString("\n") { it.text().trim() }.ifEmpty { null },
            kind = ReadingKind.COMIC,
            language = "en",
        )
    }

    /** 50 issues a page, newest first; every page of the list is read. */
    override suspend fun chapters(manga: SourceManga): List<SourceChapter> {
        val chapters = mutableListOf<SourceChapter>()
        var url: String? = "$SITE/comic/${manga.key}"
        var pages = 0
        while (url != null && pages++ < MAX_CHAPTER_PAGES) {
            val doc = page(url)
            doc.select("div.list-chapter li.row:not(.heading)").forEach { row ->
                val link = row.selectFirst("a") ?: return@forEach
                val path = link.attr("abs:href").substringAfter("/comic/${manga.key}/", "").trimEnd('/')
                if (path.isEmpty()) return@forEach
                val name = link.text().trim()
                chapters +=
                    SourceChapter(
                        key = path,
                        number =
                            ISSUE
                                .find(name)
                                ?.groupValues
                                ?.get(1)
                                ?.toDoubleOrNull() ?: -(chapters.size + 1).toDouble(),
                        title = name,
                        language = "en",
                        uploadedAtEpochMs = usDate(row.selectFirst("div.col-xs-3")?.text()),
                    )
            }
            url = doc.selectFirst("ul.pagination a[rel=next]")?.attr("abs:href")?.ifEmpty { null }
        }
        return chapters
    }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val images =
            page("$SITE/comic/${manga.key}/${chapter.key}/all")
                .select("div.page-chapter > img, li.blocks-gallery-item img")
                .mapNotNull { it.lazySrc() }
                .distinct()
        if (images.isEmpty()) throw SourceException("Capítulo sem páginas", SourceException.Kind.NOT_FOUND)
        return images.mapIndexed { i, url -> SourcePage(i, url, mapOf("Referer" to "$SITE/")) }
    }

    companion object {
        val ID = SourceId("xoxo")
        const val SITE = "https://xoxocomic.com"
        private const val MAX_CHAPTER_PAGES = 40
        private val ISSUE = Regex("#\\s*(\\d+(?:\\.\\d+)?)")
        private val SLUG = Regex("/comic/([^/?#]+)")
        private val US_DATE = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})")

        fun slugOf(url: String): String? = SLUG.find(url)?.groupValues?.get(1)

        /** "10/22/2026" (month first, no time zone) as midnight UTC. */
        fun usDate(text: String?): Long? {
            val (m, d, y) = US_DATE.find(text ?: return null)?.destructured ?: return null
            return epochDay(y.toInt(), m.toInt(), d.toInt()) * DAY_MS
        }

        private const val DAY_MS = 86_400_000L

        // Days since 1970-01-01 in the proleptic Gregorian calendar (H. Hinnant's algorithm).
        @Suppress("MagicNumber")
        private fun epochDay(
            year: Int,
            month: Int,
            day: Int,
        ): Long {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097L + doe - 719_468
        }
    }
}
