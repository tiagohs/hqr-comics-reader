package com.tiagohs.hqr.sources.portuguese

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.sources.LocaleDTO
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.sources.ParserHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class HQBRSource(
        client: OkHttpClient,
        chapterCache: ChapterCache): ParserHttpSource(client, chapterCache) {
    override val id: Long = 1L
    override val name: String = "HQBR - Leitor Online de Quadrinhos"
    override val language: LocaleDTO = LocaleDTO("Brazil", "Portuguese", "BR", "PT", Locale("PT", "BR"))
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

    override fun parseLastestComicsByElement(element: Element): ComicViewModel {
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

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = "https://hqbr.com.br/${img}"
            this.pathLink = link
        }
    }

    override fun parsePopularComicsByElement(element: Element): ComicViewModel {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = formatLink(elementSelector.attr("href"))
        }

        return ComicViewModel().apply {
            this.name = title
            this.pathLink = link
        }
    }


    override fun parseAllComicsByLetterByElement(element: Element): ComicViewModel {
        var title: String = ""
        var status: String = ""
        var link: String = ""
        var publisher: ArrayList<DefaultModelView>? = null

        element.select("td").forEach { element: Element? ->
            val linkElement = element!!.select("a").first()

            if (linkElement != null) {
                val titleRegex = "\\/hq\\/".toRegex()
                val publisherRegex = "\\/editoras\\/".toRegex()

                val linkHref = linkElement.attr("href")

                if (titleRegex.containsMatchIn(linkHref)) {linkElement.text()
                    title = linkElement.text()
                    link = formatLink(linkElement.attr("href"))
                } else if (publisherRegex.containsMatchIn(linkHref)) {
                    publisher = ArrayList()
                    publisher?.add(DefaultModelView().apply {
                        this.name = linkElement.text()
                        this.pathLink = formatLink(linkElement.attr("href"))
                    })
                }
            }

            val statusElement = element.select("span").first()

            if (statusElement != null) {
                status = ScreenUtils.getStatusConstant(statusElement.text())!!
            }
        }

        return ComicViewModel().apply {
            this.name = title
            this.pathLink = link
            this.publisher = publisher
            this.status = ScreenUtils.getStatusConstant(status)
        }
    }

    override fun parseSearchByQueryByElement(element: Element): ComicViewModel {
        return parseAllComicsByLetterByElement(element)
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        val comics = super.parseSearchByQueryResponse(response, query)

        return comics.filter({
                    comicsItem ->
                    val titleRegex = query.toLowerCase().toRegex()
                    titleRegex.containsMatchIn(comicsItem.name!!.toLowerCase())
                })
    }

    override fun parseReaderResponse(response: Response, chapterName: String?, chapterPath: String?): ChapterViewModel {
        val pages = pageListParse(response, chapterPath)

        return ChapterViewModel().apply {
            this.chapterPath = chapterPath
            this.chapterName = chapterName
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

    override fun parseComicDetailsResponse(response: Response, comicPath: String): ComicViewModel {
        val document = response.asJsoup()

        val title = document.select(".container div h2").first()?.text()
        val posterPath = document.select(".container blockquote imgLeft img").first()?.attr("src")

        var status: String = ""
        var publisher: List<DefaultModelView> = ArrayList()
        var scanlators: List<DefaultModelView> = ArrayList()
        document.select(".container div").forEach { element: Element? ->
            if (element!!.text().contains("Status")) {
                status = element.select("span").text()
            } else if (element.text().contains("Editora")) {
                publisher = element.select("a").map { element -> parseSimpleItemByElement(element) }
            } else if (element.text().contains("Equipe responsável")) {
                scanlators = element.select("a").map { element -> parseSimpleItemByElement(element) }
            }
        }

        val summary = document.select(".container blockquote").first()?.text()

        var chapters: List<ChapterViewModel> = ArrayList()
        var i = 0
        chapters = document.select(".container table tbody tr").map { element ->
            parseChapterItemByElement("td a", element, title, i++)
        }

        return ComicViewModel().apply {
            this.pathLink = comicPath
            this.posterPath = "https://hqbr.com.br/${posterPath}"
            this.name = title
            this.status = ScreenUtils.getStatusConstant(status)
            this.summary = summary
            this.publisher = publisher
            this.chapters = chapters
            this.scanlators = scanlators

            this.inicialized = true
        }
    }

    fun parseSimpleItemByElement(selector: String, element: Element): DefaultModelView {
        return this.parseSimpleItemByElement(element.select(selector).first())
    }

    fun parseChapterItemByElement(selector: String, element: Element, comicTitle: String?, sourceOrder: Int): ChapterViewModel {
        var title: String = ""
        var link: String = ""

        val elementSelected = element.select(selector).first()

        if (elementSelected != null && elementSelected.attr("href") != null) {
            link = formatLink(elementSelected.attr("href"))
            title = elementSelected.text()
        }

        return ChapterViewModel().apply {
            this.chapterName = title
            this.chapterPath = link
        }
    }

    fun parseSimpleItemByElement(element: Element?): DefaultModelView {
        var title: String = ""
        var link: String = ""

        if (element != null) {
            title = element.text()
            link = formatLink(element.attr("href"))
        }

        return DefaultModelView().apply {
            this.name = title
            this.pathLink = link
        }
    }

    fun formatLink(link: String): String {
        val p = Pattern.compile("http:\\/\\/adf.ly\\/([a-zA-Z0-9]*)\\/")
        val m = p.matcher(link)

        var linkFormated = link

        while( m.find() )
        {
            val adfly = m.group(0)
            linkFormated = link.replace(adfly, "")
        }

        return linkFormated
    }

}