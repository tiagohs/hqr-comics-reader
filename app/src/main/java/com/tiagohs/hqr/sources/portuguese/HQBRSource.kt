package com.tiagohs.hqr.sources.portuguese

import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.service.extensions.asJsoup
import com.tiagohs.hqr.sources.ParserHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.*
import java.util.regex.Pattern

class HQBRSource(client: OkHttpClient): ParserHttpSource(client) {

    override val baseUrl: String get() = "https://hqbr.com.br/"

    override fun getPublishersEndpoint(): String {
        return "$baseUrl/editoras/"
    }

    override fun getLastestComicsEndpoint(): String {
        return "$baseUrl/home"
    }

    override fun getPopularComicsEndpoint(): String {
        return "$baseUrl/home"
    }

    override fun getReaderEndpoint(hqReaderPath: String): String {
        return "$baseUrl/$hqReaderPath"
    }

    override fun getPublisherListSelector(): String {
        return "table > tbody > tr"
    }

    override fun getPopularComicsSelector(): String {
        return ".widget-area > ul li"
    }

    override fun getLastestComicsSelector(): String {
        return ".site-content .home-articles"
    }

    override fun parsePublisherByElement(element: Element): Publisher {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("td a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = elementSelector.attr("href")
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
            link = elementTitleSelector.attr("href")
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
            link = elementSelector.attr("href")
        }

        return ComicsItem(title, "", link)
    }

    override fun parsReaderResponse(response: Response): Chapter {
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

}