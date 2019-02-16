package com.tiagohs.hqr.sources.english

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
import kotlin.collections.ArrayList

class ReadComics(
        client: OkHttpClient,
        chapterCache: ChapterCache): ParserHttpSource(client, chapterCache) {

    override val id: Long = 1L
    override val name: String = "Read Comics Book Online"
    override val language: LocaleDTO = LocaleDTO("Estados Unidos", "English", "EUA", "EN", Locale("EN"))
    override val hasPageSupport: Boolean = false
    override val hasThumbnailSupport: Boolean = false
    override val baseUrl: String get() = "https://readcomicsonline.me/"

    override val homeCategoriesEndpoint: String get() = "$baseUrl/"
    override val lastestComicsEndpoint: String get() = "$baseUrl/"
    override val popularComicsEndpoint: String get() = "$baseUrl/"

    override val homeCategoriesListSelector: String get() = "#block-views-genre-select-block .content table tbody td"
    override val lastestComicsSelector: String get() = ".frontpage .cellbox"
    override val popularComicsSelector: String get() = "#preface-area .region .popbox"
    override val allComicsListSelector: String get() = "#primary #content .view-comics-list table tbody td"
    override val allComicsByPublisherSelector: String = "#primary #content-wrap .content article"
    override val searchComicsSelector: String get() = "#primary #content .view-comics-list table tbody td"

    override fun getReaderEndpoint(hqReaderPath: String): String {
        return "${hqReaderPath}?q=fullchapter"
    }

    override fun getAllComicsByPublisherEndpoint(publisherPath: String, page: Int): String {
        return "$publisherPath?page=$page"
    }

    override fun getAllComicsByScanlatorEndpoint(scanlatorPath: String): String {
        return scanlatorPath
    }

    override fun getAllComicsByLetterEndpoint(letter: String): String {
        return "$baseUrl/comics-list"
    }

    override fun getComicDetailsEndpoint(comicPath: String): String {
        return comicPath
    }

    override fun getSearchByQueryEndpoint(query: String): String {
        return "$baseUrl/comics-list"
    }


    override fun getAllComicsByGenreEndpoint(genre: String, page: Int): String {
        return "$genre?page=$page"
    }

    override fun parseHomeCategoriesByElement(element: Element): DefaultModelView? {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select(".views-field a").first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = elementSelector.attr("href")

            return DefaultModelView().apply {
                this.name = title.replace("Comic Title: ", "")
                this.pathLink = "$baseUrl${link}"
                this.type = DefaultModel.GENRE
            }
        }

        return null
    }

    override fun parseLastestComicsByElement(element: Element): ComicViewModel {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementTitleSelector = element.select(".comicbook a").first()
        val elementImageSelector = element.select(".listpic a img").first()

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
            link = elementTitleSelector.attr("href")
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title.replace("Comic Title: ", "")
            this.posterPath = "https://readcomicsonline.me${img}"
            this.pathLink = "https://readcomicsonline.me${link}"
        }
    }

    override fun parsePopularComicsByElement(element: Element): ComicViewModel {
        var title: String = ""
        var img: String = ""
        var link: String = ""

        val elementTitleSelector = element.select(".popname a").first()
        val elementImageSelector = element.select(".poppic a img").first()

        if (elementTitleSelector != null) {
            title = elementTitleSelector.text()
            link = elementTitleSelector.attr("href")
        }

        if (elementImageSelector != null) {
            img = elementImageSelector.attr("src")
        }

        return ComicViewModel().apply {
            this.name = title.replace("Comic Title: ", "")
            this.posterPath = "https://readcomicsonline.me${img}"
            this.pathLink = "https://readcomicsonline.me${link}"
        }
    }

    override fun parseAllComicsByLetterByElement(element: Element): ComicViewModel? {
        var title: String = ""
        var link: String = ""

        val elementSelector = element.select("a")?.first()

        if (elementSelector != null) {
            title = elementSelector.text()
            link = elementSelector.attr("href")

            return ComicViewModel().apply {
                this.name = title.replace("Comic Title: ", "")
                this.pathLink = "$baseUrl${link}"
            }
        }

        return null
    }

    override fun parseAllComicsByPublisherByElement(element: Element): ComicViewModel? {
        val content = element.select(".field-name-body .splash")?.first()

        val posterPath = content?.select(".pic img")?.first()?.attr("src")
        val titleSelector = element.select("header h2 a")?.first()

        var name: String = ""
        var link: String = ""

        if (titleSelector != null) {
            name = titleSelector.text()
            link = titleSelector.attr("href")

        }

        return ComicViewModel().apply {
            this.name = name.replace("Comic Title: ", "")
            this.posterPath = "https://readcomicsonline.me${posterPath}"
            this.pathLink = "$baseUrl${link}"
        }

    }

    override fun parseAllComicsByGenreByElement(element: Element): ComicViewModel? {

        val posterPath = element.select(".field-name-field-pic img")?.first()?.attr("src")
        val titleSelector = element.select("header h2 a")?.first()

        var name: String = ""
        var link: String = ""

        if (titleSelector != null) {
            name = titleSelector.text()
            link = titleSelector.attr("href")

        }

        return ComicViewModel().apply {
            this.name = name.replace("Comic Title: ", "")
            this.posterPath = "https://readcomicsonline.me${posterPath}"
            this.pathLink = "$baseUrl${link}"
        }
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        val comics = super.parseSearchByQueryResponse(response, query)

        return comics.filter({
            comicsItem ->
            val titleRegex = query.toLowerCase().toRegex()
            titleRegex.containsMatchIn(comicsItem.name!!.toLowerCase())
        })
    }

    override fun parseSearchByQueryByElement(element: Element): ComicViewModel {
        return parseSearchByQueryByElement(element)
    }

    override fun parseComicDetailsResponse(response: Response, comicPath: String): ComicViewModel? {
        
        var document: Document? = null
        try {
            document = response.asJsoup()
        } catch(ex: Exception) {
            Timber.e(ex)
        }
        
        if (document != null) {
            val content = document.select("#primary #content .region-content .content").first()

            var posterPath = content.select(".field-name-body .pic img").first()?.attr("src")

            if (posterPath === null) {
                posterPath = content.select(".field-name-field-pic .field-item img").first()?.attr("src")
            }

            var summary = content.select(".field-name-field-synopsis .field-item").first()?.text()

            if (summary === null) {
                summary = content.select(".field-name-body .summary").first()?.text()
            }

            var name: String? = ""
            var publicationDate: String? = ""
            var status: String? = ""

            content.select(".field-name-body .info").forEach { element: Element? ->
                val title = element?.text() ?: ""

                if (title.contains("Comic Title")) {
                    name = element?.text()
                } else if (title.contains("Publication Run")) {
                    publicationDate = element?.text()
                } else if (title.contains("Status")) {
                    status = element?.text()
                }
            }

            val genres: ArrayList<DefaultModelView> = ArrayList()
            content.select(".field-name-field-genres .field-item").forEach { element: Element ->
                genres.add(parseSimpleItemByElement(element.select("a").first(), DefaultModel.GENRE))
            }

            val publishers: ArrayList<DefaultModelView> = ArrayList()
            content.select(".field-name-field-publisher .field-item").forEach { element: Element ->
                publishers.add(parseSimpleItemByElement(element.select("a").first(), DefaultModel.GENRE))
            }

            val chapters: ArrayList<ChapterViewModel> = ArrayList()
            content.select("#chapterlist .chapter").forEach { element: Element ->
                chapters.add(parseChapterByElement(element.select("a").first(), DefaultModel.GENRE))
            }

            return ComicViewModel().apply {
                this.pathLink = comicPath
                this.posterPath = "https://readcomicsonline.me${posterPath}"
                this.name = name?.replace("Comic Title: ", "")
                this.publicationDate = publicationDate
                this.status = ScreenUtils.getStatusConstant(status)
                this.summary = summary
                this.publisher = publishers
                this.chapters = chapters
                this.genres = genres

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
            val containers = document.select("#omv table")
            val imagesContainer = containers?.select("table tbody tr td")?.get(0)

            imagesContainer?.select("img")?.forEach { element ->
                val imageUrl = element.attr("src")

                if (!imageUrl.isNullOrEmpty()) {
                    pages.add(Page(pages.size, chapterPath!!, "https://readcomicsonline.me/reader/$imageUrl"))
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
            this.name = title.replace("Comic Title: ", "")
            this.pathLink = "$baseUrl${link}"
            this.type = type
        }
    }

    fun parseChapterByElement(element: Element?, type: String): ChapterViewModel {
        var title: String = ""
        var link: String = ""

        if (element != null) {
            title = element.text()
            link = element.attr("href")
        }

        return ChapterViewModel().apply {
            this.chapterName = title.replace("Comic Title: ", "")
            this.chapterPath = link
        }
    }

}