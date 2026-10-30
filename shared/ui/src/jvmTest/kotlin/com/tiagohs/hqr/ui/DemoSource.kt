package com.tiagohs.hqr.ui

import com.tiagohs.hqr.domain.model.MangaPage
import com.tiagohs.hqr.domain.model.PublicationStatus
import com.tiagohs.hqr.domain.model.ReadingKind
import com.tiagohs.hqr.domain.model.SourceChapter
import com.tiagohs.hqr.domain.model.SourceId
import com.tiagohs.hqr.domain.model.SourceManga
import com.tiagohs.hqr.domain.model.SourcePage
import com.tiagohs.hqr.domain.source.CatalogSource
import com.tiagohs.hqr.domain.source.ImageSource
import com.tiagohs.hqr.domain.source.SearchSource
import com.tiagohs.hqr.domain.source.SourceFilter
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** A fictional source with drawn covers and pages: no network, no copyrighted art. */
class DemoSource(
    private val pageCount: Int = 22,
    private val kind: ReadingKind = ReadingKind.MANGA,
    /** Adds chapter 16, licensed: only on the publisher's site (RF-11). */
    private val externalChapter: Boolean = false,
) : CatalogSource,
    SearchSource,
    ImageSource {
    override val id = SourceId("demo")
    override val name = "MangaDex"
    override val languages = listOf("pt-br", "en")

    private val titles =
        listOf(
            "Lâmina do Norte",
            "Kaiju Express",
            "Céu de Papel",
            "Moça do Farol",
            "Distrito Zero",
            "Gato Astronauta",
            "Ronin 2099",
            "Noite Ciana",
            "Tinta e Osso",
        )
    private val colors = listOf(0x2B3A4A, 0x1B8A5A, 0xF2E6D0, 0x3A2F45, 0x2A2A2E, 0x13253F, 0x3B1F1F, 0x0F2E36, 0xE9E4DA)

    fun manga(i: Int) =
        SourceManga(
            sourceId = id,
            key = "m$i",
            title = titles[i % titles.size],
            coverUrl = "demo://cover/$i",
            url = "https://example.org/m$i",
            authors = listOf("Rui Takeda"),
            artists = listOf("Mei Lin"),
            status = PublicationStatus.ONGOING,
            genres = listOf("Aventura", "Fantasia", "Seinen"),
            description =
                "Depois que o último farol do norte se apaga, uma ferreira que forja lâminas para fantasmas " +
                    "precisa atravessar o gelo antes da longa noite.",
            language = "pt-br",
            kind = kind,
        )

    override suspend fun popular(page: Int) = MangaPage((0 until 9).map { manga(it) }, false)

    override suspend fun latest(page: Int) = popular(page)

    var lastFilters: List<SourceFilter> = emptyList()

    override suspend fun filters() =
        com.tiagohs.hqr.sources.mangadex.MangaDexFilters.defaults(
            listOf("Action", "Comedy", "Horror", "Romance").map { SourceFilter.Option(it.lowercase(), it) },
        )

    override suspend fun search(
        query: String,
        filters: List<SourceFilter>,
        page: Int,
    ): MangaPage {
        lastFilters = filters
        return popular(page)
    }

    override suspend fun details(manga: SourceManga) = manga(manga.key.removePrefix("m").toInt())

    override suspend fun chapters(manga: SourceManga) =
        (1..15).map {
            SourceChapter(
                "c$it",
                it.toDouble(),
                title = "Capítulo $it",
                group =
                    if (it % 3 ==
                        0
                    ) {
                        "Grupo Kitsune"
                    } else {
                        "Scan Aurora"
                    },
                uploadedAtEpochMs = 1_790_000_000_000 - it * 86_400_000L,
            )
        } +
            if (externalChapter) {
                listOf(
                    SourceChapter("c16", 16.0, title = "Capítulo 16", externalUrl = "https://publisher.example/c16"),
                )
            } else {
                emptyList()
            }

    override suspend fun pages(
        manga: SourceManga,
        chapter: SourceChapter,
    ) = (0 until pageCount).map { SourcePage(it, "demo://page/$it") }

    override suspend fun image(
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        val n = url.substringAfterLast('/').toInt()
        val cover = url.contains("cover")
        val w = if (cover) 200 else 600
        val h = if (cover) 300 else 900
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(if (cover) colors[n % colors.size] else 0xF6F1E7)
        g.fillRect(0, 0, w, h)
        g.color = java.awt.Color(0xE9DCC3)
        g.fillOval(w / 4, h / 8, w / 2, w / 2)
        g.color = java.awt.Color(0x16161A)
        g.fillPolygon(
            intArrayOf(0, w / 4, w / 2, 3 * w / 4, w, w, 0),
            intArrayOf(h * 3 / 4, h * 2 / 3, h * 3 / 4, h * 3 / 5, h * 3 / 4, h, h),
            7,
        )
        g.dispose()
        return File.createTempFile("img", ".png").also { ImageIO.write(img, "png", it) }.readBytes()
    }
}
