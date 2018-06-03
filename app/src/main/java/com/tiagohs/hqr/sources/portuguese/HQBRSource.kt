package com.tiagohs.hqr.sources.portuguese

import com.tiagohs.hqr.models.sources.*
import com.tiagohs.hqr.service.extensions.asJsoup
import com.tiagohs.hqr.sources.ParserHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class HQBRSource(client: OkHttpClient): ParserHttpSource(client) {
    override val baseUrl: String get() = "https://hqbr.com.br/"

    override val publishersEndpoint: String get() = "$baseUrl/editoras/"
    override val lastestComicsEndpoint: String get() = "$baseUrl/home"
    override val popularComicsEndpoint: String get() = "$baseUrl/home"

    override val publisherListSelector: String get() = "table > tbody > tr"
    override val lastestComicsSelector: String get() = ".site-content .home-articles"
    override val popularComicsSelector: String get() = ".widget-area > ul li"

    override val comicPublisherSelector: String get() = ".container div"
    override val comicTitleSelector: String get() = ".container div h2"
    override val comicPosterPathSelector: String get() = ".container blockquote imgLeft img"
    override val comicStatusSelector: String get() = ".container div"
    override val comicSummarySelector: String get() = ".container blockquote"
    override val comicPublicationDateSelector: String get() = ""

    override val comicChaptersSelector: String get() = ".container table tbody tr"
    override val comicGenresSelector: String get() = ""
    override val comicWritersSelector: String get() = ""
    override val comicArtistsSelector: String get() = ""
    override val comicScanlatorsSelector: String get() = ".container div:nth-child(1) a"

    override val comicPublisherItemSelector: String get() = "a"
    override val comicChapterItemSelector: String get() = "td a"
    override val comicScanlatorItemSelector: String get() = ""
    override val comicGenreItemSelector: String get() = ""
    override val comicWriterItemSelector: String get() = ""
    override val comicArtistItemSelector: String get() = ""

    override fun getReaderEndpoint(hqReaderPath: String): String {
        return "$baseUrl/$hqReaderPath"
    }

    override fun getComicDetailsEndpoint(comicPath: String): String {
        return comicPath
    }

    override fun parsePublisherByElement(element: Element): Publisher {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("td a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = formatLink(elementSelector.attr("href"))
        }

        return Publisher(title, link)
    }

    override fun parseLastestComicsByElement(element: Element): ComicsItem {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementTitleSelector = element.select(".entry-title a").first()
        val elementImageSelector = element.select(".columns a img").first()

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
            link = formatLink(elementTitleSelector.attr("href"))
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicsItem(title, img, link)
    }

    override fun parsePopularComicsByElement(element: Element): ComicsItem {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = formatLink(elementSelector.attr("href"))
        }

        return ComicsItem(title, "", link)
    }

    override fun parseReaderResponse(response: Response): Chapter {
        val document = response!!.asJsoup()
        val script = document.select("#chapter_pages script").first() // Get the script part

        val p = Pattern.compile("pages = \\[((.*))\\]") // Regex for the value of the html
        val m = p.matcher(script.html())

        var pages: List<String> = ArrayList()

        while( m.find() )
        {
            pages = m.group(1).replace("\"", "").split(",")
        }

        return Chapter(pages)
    }


    override fun parseComicDetailsResponse(response: Response): Comic {
        val document = response.asJsoup()

        val title = if (comicTitleSelector.isNotEmpty()) document.select(comicTitleSelector).first().text() else null
        val posterPath = if (comicPosterPathSelector.isNotEmpty()) document.select(comicPosterPathSelector).first().attr("src") else null

        var status: String = ""
        if (comicStatusSelector.isNotEmpty())
            document.select(comicStatusSelector).forEach { element: Element? ->
                if (element!!.text().contains("Status:")) {
                    status = element.select("span").text()
                }
            }

        val summary = if (comicSummarySelector.isNotEmpty()) document.select(comicSummarySelector).first().text() else null
        val publicationDate = if (comicPublicationDateSelector.isNotEmpty()) document.select(comicPublicationDateSelector).first().text() else null

        var publisher: List<SimpleItem> = ArrayList()

        if (comicPublisherSelector.isNotEmpty())
            document.select(comicPublisherSelector).forEach { element: Element? ->
                if (element!!.text().contains("Editora")) {
                    publisher = element.select(comicPublisherItemSelector).map { element -> parseSimpleItemByElement(element) }
                }
            }

        val genres = if (comicGenresSelector.isNotEmpty())
            document.select(comicGenresSelector).map { element -> parseSimpleItemByElement(comicGenreItemSelector, element)
            } else ArrayList()

        val chapters = if (comicChaptersSelector.isNotEmpty())
            document.select(comicChaptersSelector).map { element -> parseSimpleItemByElement(comicChapterItemSelector, element)
            } else ArrayList()

        val writers = if (comicWritersSelector.isNotEmpty())
            document.select(comicWritersSelector).map { element -> parseSimpleItemByElement(comicWriterItemSelector, element)
            } else ArrayList()

        val artists = if (comicArtistsSelector.isNotEmpty())
            document.select(comicArtistsSelector).map { element -> parseSimpleItemByElement(comicArtistItemSelector, element)
            } else ArrayList()

        val scanlators: List<SimpleItem> = if (comicScanlatorsSelector.isNotEmpty())
            document.select(comicScanlatorsSelector).map { element -> parseSimpleItemByElement(comicScanlatorItemSelector, element)
            } else ArrayList()

        return Comic(title, posterPath, status, publisher, genres, writers, artists, chapters, summary, publicationDate, scanlators)
    }

    fun parseSimpleItemByElement(selector: String, element: Element): SimpleItem {
        return this.parseSimpleItemByElement(element.select(selector).first())
    }

    fun parseSimpleItemByElement(element: Element): SimpleItem {
        var title: String = ""
        var link: String = ""

        if (element != null) {
            title = element.text()
            link = formatLink(element.attr("href"))
        }

        return SimpleItem(title, link)
    }

    fun formatLink(link: String): String {
        val p = Pattern.compile("http:\\/\\/adf.ly\\/([a-zA-Z0-9]*)\\/")
        val m = p.matcher(link)

        var pages: List<String> = ArrayList()
        var linkFormated = link

        while( m.find() )
        {
            val adfly = m.group(0)
            linkFormated = link.replace(adfly, "")
        }

        return linkFormated
    }

}