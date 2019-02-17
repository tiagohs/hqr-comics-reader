package com.tiagohs.hqr.sources.portuguese

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.sources.LocaleDTO
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.sources.ParserHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.*

class HQUltimate(
        client: OkHttpClient,
        chapterCache: ChapterCache): ParserHttpSource(client, chapterCache) {

    override val id: Long = 2L
    override val name: String = "HQ Ultimate"
    override val language: LocaleDTO = LocaleDTO("Brasil", "Português", "BR", "PT", Locale("pt_BR"))
    override val hasPageSupport: Boolean = true
    override val hasThumbnailSupport: Boolean = true
    override val baseUrl: String get() = "http://hqultimate.site/"

    override val homeCategoriesEndpoint: String get() = "$baseUrl/"
    override val lastestComicsEndpoint: String get() = "$baseUrl/"
    override val popularComicsEndpoint: String get() = "$baseUrl"

    override val homeCategoriesListSelector: String get() = ""
    override val lastestComicsSelector: String get() = ".row .col-md-8 .row .col-md-3"
    override val popularComicsSelector: String get() = ".row .col-md-4 .media.lancamento-linha"
    override val allComicsListSelector: String get() = ".row .col-md-8 .row .col-md-3"
    override val allComicsByPublisherSelector: String = ".row .col-md-8 .row .col-md-3"
    override val searchComicsSelector: String get() = ".row .col-md-8 .row .col-md-3"

    override fun getReaderEndpoint(hqReaderPath: String): String {
        return "${hqReaderPath}"
    }

    override fun getAllComicsByPublisherEndpoint(publisherPath: String, page: Int): String {
        return "$publisherPath/hqs/a-z/$page/*"
    }

    override fun getAllComicsByScanlatorEndpoint(scanlatorPath: String): String {
        return scanlatorPath
    }

    override fun getAllComicsByLetterEndpoint(letter: String, page: Int): String {
        return "$baseUrl/hqs/a-z/${page + 1}/*"
    }

    override fun getComicDetailsEndpoint(comicPath: String): String {
        return comicPath
    }

    override fun getSearchByQueryEndpoint(query: String): String {
        return "$baseUrl/busca?pesquisa=${query}"
    }


    override fun getAllComicsByGenreEndpoint(genre: String, page: Int): String {
        return "$genre?page=$page"
    }

    override fun parseHomeCategoriesByElement(element: Element): DefaultModelView? {


        return null
    }

    override fun parseLastestComicsByElement(element: Element): ComicViewModel {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select("article a").first()
        val elementTitleSelector = element.select("article h3").first()
        val elementImageSelector = element.select("article a img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parsePopularComicsByElement(element: Element): ComicViewModel {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select(".media-left").first()
        val elementTitleSelector = element.select(".media-body h4 a").first()
        val elementImageSelector = element.select(".media-left img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parseAllComicsByLetterByElement(element: Element): ComicViewModel? {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select("a").first()
        val elementTitleSelector = element.select("a")?.get(1)
        val elementImageSelector = element.select("a img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parseAllComicsByPublisherByElement(element: Element): ComicViewModel? {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select("a").first()
        val elementTitleSelector = element.select("a")?.get(1)
        val elementImageSelector = element.select("a img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parseAllComicsByGenreByElement(element: Element): ComicViewModel? {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select("a").first()
        val elementTitleSelector = element.select("a")?.get(1)
        val elementImageSelector = element.select("a img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        return super.parseSearchByQueryResponse(response, query)
    }

    override fun parseSearchByQueryByElement(element: Element): ComicViewModel {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementLink = element.select("a").first()
        val elementTitleSelector = element.select("a")?.get(1)
        val elementImageSelector = element.select("a img").first()

        if (elementLink != null) {
            link = elementLink.attr("href")
        }

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title
            this.posterPath = img
            this.pathLink = link
        }
    }

    override fun parseComicDetailsResponse(response: Response, comicPath: String): ComicViewModel? {
        var document: Document? = null

        try {
            document = response.asJsoup()
        } catch(ex: Exception) {
            Timber.e(ex)
        }

        if (document != null) {
            val content = document.select(".row .tamanho-bloco-perfil").first()

            val posterPath = content.select(".col-md-4 img").first()?.attr("src")
            val summary = content.select(".col-md-8 .panel .panel-body").first()?.text()

            val name: String? = content.select(".col-md-12 h2")?.first()?.text()
            var publicationDate: String? = ""
            var status: String? = ""

            val publishers: ArrayList<DefaultModelView> = ArrayList()

            content.select(".col-md-8 .media-heading").forEach { element: Element? ->
                val title = element?.select(".subtit-manga")?.text() ?: ""

                if (title.contains("Editora:")) {
                    publishers.add(
                            parseSimpleItemByElement(
                                    element?.select("a")?.first(),
                                    DefaultModel.PUBLISHER
                            )
                    )
                } else if (title.contains("Ano de Lançamento:")) {
                    val year = element?.text()

                    if (year != null) {
                        if (!year.contains("----")) {
                            publicationDate = year
                        }
                    }
                } else if (title.contains("Status:")) {
                    status = element?.select(".label")?.text()
                }
            }

            val chapters: ArrayList<ChapterViewModel> = ArrayList()
            content.select(".row .col-xs-6").forEach { element: Element ->
                chapters.add(parseChapterByElement(element.select("article a").first(), DefaultModel.GENRE))
            }

            return ComicViewModel().apply {
                this.pathLink = comicPath
                this.posterPath = posterPath
                this.name = name
                this.publicationDate = publicationDate?.replace("Ano de Lançamento: ", "")
                this.status = ScreenUtils.getStatusConstant(status)
                this.summary = summary
                this.publisher = publishers
                this.chapters = chapters

                this.inicialized = true
            }
        }

        return null
    }

    override fun parseReaderResponse(response: Response, chapterPath: String?): ChapterViewModel {
        val pages = pageListParse(response, chapterPath)

        return ChapterViewModel().apply {
            this.chapterPath = chapterPath
            this.pages = pages
        }
    }

    override fun pageListParse(response: Response, chapterPath: String?): List<Page> {
        val pages: ArrayList<Page> = ArrayList()

        try {
            val document = response.asJsoup()
            val container = document.select(".col-md-12 #image")

            container?.select(".item")?.forEach { element ->
                val imageUrl = element?.select("img")?.first()?.attr("data-lazy")

                if (!imageUrl.isNullOrEmpty()) {
                    pages.add(Page(pages.size, chapterPath!!, imageUrl))
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return pages
    }

    fun parseSimpleItemByElement(element: Element?, type: String): DefaultModelView {
        var title: String = ""
        var link: String = ""

        if (element != null) {
            title = element.text()
            link = element.attr("href")
        }

        return DefaultModelView().apply {
            this.name = title
            this.pathLink = link
            this.type = type
        }
    }

    fun parseChapterByElement(element: Element?, type: String): ChapterViewModel {
        var title: String = ""
        var link: String = ""

        if (element != null) {
            link = element.attr("href")
        }

        val titleElement = element?.select("h3")

        if (titleElement != null) {
            title = titleElement.text()
        }

        return ChapterViewModel().apply {
            this.chapterName = title
            this.chapterPath = link
        }
    }
}