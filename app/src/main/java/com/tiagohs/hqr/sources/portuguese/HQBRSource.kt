package com.tiagohs.hqr.sources.portuguese

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.sources.*
import com.tiagohs.hqr.models.viewModels.ComicsListModel
import com.tiagohs.hqr.sources.ParserHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class HQBRSource(
        client: OkHttpClient,
        chapterCache: ChapterCache): ParserHttpSource(client, chapterCache) {
    override val id: Long = 0L
    override val name: String = "HQBR"
    override val language: Locale = Locale(java.util.Locale.getDefault())
    override val hasPageSupport: Boolean = false
    override val hasThumbnailSupport: Boolean = false
    override val baseUrl: String get() = "https://hqbr.com.br/"

    override val publishersEndpoint: String get() = "$baseUrl/editoras/"
    override val lastestComicsEndpoint: String get() = "$baseUrl/home"
    override val popularComicsEndpoint: String get() = "$baseUrl/home"

    override val publisherListSelector: String get() = "table > tbody > tr"
    override val lastestComicsSelector: String get() = ".site-content .home-articles"
    override val popularComicsSelector: String get() = ".widget-area > ul li"
    override val allComicsListSelector: String get() = "table > tbody > tr"
    override val searchComicsSelector: String get() = "table > tbody > tr"

    override fun getReaderEndpoint(hqReaderPath: String): String {
        return "$baseUrl/$hqReaderPath"
    }

    override fun getAllComicsByPublisherEndpoint(publisherPath: String): String {
        return "$baseUrl/$publisherPath"
    }

    override fun getAllComicsByScanlatorEndpoint(scanlatorPath: String): String {
        return "$baseUrl/$scanlatorPath"
    }

    override fun getAllComicsByLetterEndpoint(letter: String): String {
        return "$baseUrl/hqs?letter=$letter"
    }

    override fun getComicDetailsEndpoint(comicPath: String): String {
        val urlBaseRegex = "https:\\/\\/hqbr.com.br\\/".toRegex()

        if (urlBaseRegex.containsMatchIn(comicPath))
            return comicPath
        else
            return "$baseUrl/$comicPath"
    }

    override fun getSearchByQueryEndpoint(query: String): String {
        return "$baseUrl/hqs?letter=all"
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

        return ComicsItem(title, img, link, "", "")
    }

    override fun parsePopularComicsByElement(element: Element): ComicsItem {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = formatLink(elementSelector.attr("href"))
        }

        return ComicsItem(title, "", link, "", "")
    }


    override fun parseAllComicsByLetterByElement(element: Element): ComicsItem {
        var title: String = ""
        var status: String = ""
        var link: String = ""
        var publisher: String = ""

        element.select("td").forEach { element: Element? ->
            val linkElement = element!!.select("a").first()

            if (linkElement != null) {
                val titleRegex = "\\/hq\\/".toRegex()
                val publisherRegex = "\\/editoras\\/".toRegex()

                val linkHref = linkElement.attr("href")

                if (titleRegex.containsMatchIn(linkHref)) {
                    title = linkElement.text()
                    link = formatLink(linkElement.attr("href"))
                } else if (publisherRegex.containsMatchIn(linkHref)) {
                    publisher = linkElement.text()
                }
            }

            val statusElement = element.select("span").first()

            if (statusElement != null) {
                status = ScreenUtils.getStatusConstant(statusElement.text())!!
            }
        }

        return ComicsItem(title, "", link, publisher, status)
    }

    override fun parseSearchByQueryByElement(element: Element): ComicsItem {
        return parseAllComicsByLetterByElement(element)
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): ComicsListModel {
        val comicsModel = super.parseSearchByQueryResponse(response, query)

        comicsModel.comics = comicsModel.comics.filter({
                                comicsItem ->
                                    val titleRegex = query.toLowerCase().toRegex()
                            titleRegex.containsMatchIn(comicsItem.title.toLowerCase())
                            })

        return comicsModel
    }

    override fun parseReaderResponse(response: Response, chapterName: String?, chapterPath: String?, comicId: String?): Chapter {
        val pages = pageListParse(response, chapterPath)

        return Chapter().apply {
            this.id = chapterPath
            this.comicId = comicId
            this.name = chapterName
            this.pages = pages
        }
    }

    override fun pageListParse(response: Response, chapterPath: String?): List<Page> {
        val document = response.asJsoup()
        val script = document.select("#chapter_pages script").first() // Get the script part

        val p = Pattern.compile("pages = \\[((.*))\\]") // Regex for the value of the html
        val m = p.matcher(script.html())

        val pages: ArrayList<Page> = ArrayList()

        while( m.find() )
        {
            val imagesUrl = m.group(1).replace("\"", "").split(",")

            imagesUrl.forEach { imageUrl: String ->
                pages.add(Page(pages.size, chapterPath!!, "$baseUrl$imageUrl"))
            }

        }

        return pages
    }

    override fun parseComicDetailsResponse(response: Response, comicPath: String): Comic {
        val document = response.asJsoup()

        val title = document.select(".container div h2").first().text()
        val posterPath = document.select(".container blockquote imgLeft img").first().attr("src")

        var status: String = ""
        document.select(".container div").forEach { element: Element? ->
            if (element!!.text().contains("Status:")) {
                status = element.select("span").text()
            }
        }

        val summary = document.select(".container blockquote").first().text()

        var publisher: List<SimpleItem> = ArrayList()
        document.select(".container div").forEach { element: Element? ->
            if (element!!.text().contains("Editora")) {
                publisher = element.select("a").map { element -> parseSimpleItemByElement(element) }
            }
        }

        var scanlators: List<SimpleItem> = ArrayList()
        document.select(".container div").forEach { element: Element? ->
            if (element!!.text().contains("Equipe responsável")) {
                scanlators = element.select("a").map { element -> parseSimpleItemByElement(element) }
            }
        }

        val chapters = ArrayList<Chapter>()
        var i = 0
        document.select(".container table tbody tr").map { element ->
            parseChapterItemByElement("td a", element, title, i++)
        }

        return Comic().apply {
            this.id = comicPath
            this.title = title
            this.pathLink = comicPath
            this.posterPath = posterPath
            this.status = status
            this.publisher = publisher
            this.chapters = chapters
            this.summary = summary
            this.scanlators = scanlators
            this.sourceId = this@HQBRSource.id
        }
    }

    fun parseSimpleItemByElement(selector: String, element: Element): SimpleItem {
        return this.parseSimpleItemByElement(element.select(selector).first())
    }

    fun parseChapterItemByElement(selector: String, element: Element, comicTitle: String?, sourceOrder: Int): Chapter {
        var title: String = ""
        var link: String = ""

        val elementSelected = element.select(selector).first()

        if (elementSelected != null) {
            title = elementSelected.text()
            link = formatLink(elementSelected.attr("href"))
        }

        return Chapter().apply {
            this.name = title
            this.chapterPath = link
            this.sourceOrder = sourceOrder
        }
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