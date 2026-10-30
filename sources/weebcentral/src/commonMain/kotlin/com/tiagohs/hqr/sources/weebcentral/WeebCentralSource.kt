package com.tiagohs.hqr.sources.weebcentral

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
import com.tiagohs.hqr.domain.model.isoEpochMs
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceException
import com.tiagohs.hqr.domain.source.SourceFilter
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Weeb Central (RF-103): English manga read from the site's pages, one request every 2 seconds. Popular, latest and the
 * search are the same listing with another order; the work key is the series id (a ULID), the chapter's too.
 */
class WeebCentralSource(
    private val http: HttpClient,
    clock: Clock = Clock.System,
    sleep: suspend (Duration) -> Unit = { delay(it) },
) : CatalogSource,
    SearchSource {
    override val id: SourceId = ID
    override val name: String = "Weeb Central"
    override val languages: List<String> = listOf("en")

    override fun webUrl(query: String): String = if (query.isBlank()) SITE else "$SITE/search?text=${query.encodeURLParameter()}"

    private val limiter = RateLimiter(1, 2.seconds, clock, sleep)

    private suspend fun page(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): Document {
        limiter.acquire()
        val html = networkCall { http.get(url, block).ensureOk().bodyAsText() }
        return Ksoup.parse(html, url)
    }

    private suspend fun list(
        page: Int,
        sort: String,
        query: String = "",
    ): MangaPage {
        val doc =
            page("$SITE/search/data") {
                if (query.isNotBlank()) parameter("text", query.replace(SYMBOLS, " ").trim())
                parameter("sort", sort)
                parameter("order", "Descending")
                parameter("official", "Any")
                parameter("anime", "Any")
                parameter("adult", "False")
                parameter("display_mode", "Full Display")
                parameter("limit", PAGE)
                parameter("offset", (page.coerceAtLeast(1) - 1) * PAGE)
            }
        // Each card links twice to its series: one work per id.
        val items =
            doc
                .select("article > section > a[href*=/series/]")
                .mapNotNull { it.toManga() }
                .distinctBy { it.key }
        return MangaPage(items, doc.selectFirst("button[hx-get]") != null)
    }

    private fun Element.toManga(): SourceManga? {
        val key = seriesId(attr("abs:href")) ?: return null
        val image = selectFirst("img")
        val title =
            selectFirst("div.truncate")?.text()?.trim()?.ifEmpty { null }
                ?: image
                    ?.attr("alt")
                    ?.removeSuffix(" cover")
                    ?.trim()
                    ?.ifEmpty { null }
                ?: return null
        return SourceManga(sourceId = ID, key = key, title = title, coverUrl = cover(this), url = "$SITE/series/$key", language = "en")
    }

    /** The webp of the size the card asks, else the jpeg fallback. */
    private fun cover(scope: Element): String? =
        scope
            .selectFirst("source[srcset]")
            ?.attr("abs:srcset")
            ?.substringBefore(' ')
            ?.replace("/small/", "/normal/")
            ?: scope.selectFirst("img[src]")?.attr("abs:src")

    override suspend fun popular(page: Int): MangaPage = list(page, "Popularity")

    override suspend fun latest(page: Int): MangaPage = list(page, "Latest Updates")

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage = list(page, if (query.isBlank()) "Popularity" else "Best Match", query)

    override suspend fun details(manga: SourceManga): SourceManga {
        val doc = page("$SITE/series/${manga.key}")
        val columns = doc.select("section[x-data] > section")
        val left = columns.getOrNull(0) ?: throw SourceException("Página da obra mudou", SourceException.Kind.PARSE)
        val right = columns.getOrNull(1) ?: left

        fun field(
            scope: Element,
            label: String,
        ) = scope.select("ul > li").firstOrNull { it.selectFirst("strong")?.text()?.contains(label) == true }
        return manga.copy(
            title =
                right
                    .selectFirst("h1")
                    ?.text()
                    ?.trim()
                    ?.ifEmpty { null } ?: manga.title,
            coverUrl = cover(left) ?: manga.coverUrl,
            url = "$SITE/series/${manga.key}",
            authors = field(left, "Author")?.select("a")?.map { it.text().trim() }.orEmpty(),
            genres = field(left, "Tag")?.select("a")?.map { it.text().trim() }.orEmpty(),
            status =
                when (field(left, "Status")?.selectFirst("a")?.text()?.trim()) {
                    "Ongoing" -> PublicationStatus.ONGOING
                    "Complete" -> PublicationStatus.COMPLETED
                    "Hiatus" -> PublicationStatus.HIATUS
                    "Canceled" -> PublicationStatus.CANCELLED
                    else -> PublicationStatus.UNKNOWN
                },
            kind =
                when (field(left, "Type")?.selectFirst("a")?.text()?.trim()) {
                    "Manhwa", "Manhua" -> ReadingKind.MANHWA
                    "OEL" -> ReadingKind.COMIC
                    else -> ReadingKind.MANGA
                },
            description = field(right, "Description")?.selectFirst("p")?.wholeText()?.trim(),
            language = "en",
        )
    }

    override suspend fun chapters(manga: SourceManga): List<SourceChapter> =
        page("$SITE/series/${manga.key}/full-chapter-list")
            .select("div[x-data] > a[href*=/chapters/]")
            .mapNotNull { link ->
                val key =
                    link
                        .attr("abs:href")
                        .substringAfter("/chapters/")
                        .substringBefore('/')
                        .ifEmpty { return@mapNotNull null }
                val name =
                    link
                        .selectFirst("span.grow > span")
                        ?.text()
                        ?.trim()
                        .orEmpty()
                SourceChapter(
                    key = key,
                    number = chapterNumber(name),
                    title = name.ifEmpty { null },
                    language = "en",
                    group = if (link.selectFirst("img[src*=official]") != null) OFFICIAL else null,
                    uploadedAtEpochMs = isoEpochMs(link.selectFirst("time[datetime]")?.attr("datetime")),
                )
            }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ): List<SourcePage> {
        val images =
            page("$SITE/chapters/${chapter.key}/images") {
                parameter("is_prev", "False")
                parameter("reading_style", "long_strip")
            }.select("section#chapter-images > img, section[x-data] > img").map { it.attr("abs:src") }.filter { it.isNotBlank() }.distinct()
        if (images.isEmpty()) throw SourceException("Capítulo sem páginas", SourceException.Kind.NOT_FOUND)
        return images.mapIndexed { i, url -> SourcePage(i, url, mapOf("Referer" to "$SITE/")) }
    }

    companion object {
        val ID = SourceId("weebcentral")
        const val SITE = "https://weebcentral.com"
        private const val PAGE = 32
        private const val OFFICIAL = "Official"
        private val SYMBOLS = Regex("[!#:(),-]")
        private val LABELED = Regex("(?i)(?:chapter|ch\\.?|episode|ep\\.?)\\s*(\\d+(?:\\.\\d+)?)")
        private val NUMBER = Regex("\\d+(?:\\.\\d+)?")

        /** "Vol. 2 Chapter 5" is chapter 5; with no number at all, [NO_NUMBER] (never matched by a migration). */
        fun chapterNumber(name: String): Double =
            LABELED
                .find(name)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
                ?: NUMBER
                    .findAll(name)
                    .lastOrNull()
                    ?.value
                    ?.toDoubleOrNull()
                ?: NO_NUMBER

        const val NO_NUMBER = -1.0
        private val SERIES = Regex("/series/([0-9A-Z]{26})")

        fun seriesId(url: String): String? = SERIES.find(url)?.groupValues?.get(1)
    }
}
