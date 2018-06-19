package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.viewModels.ComicsListModel
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element

abstract class ParserHttpSource(
        private var client: OkHttpClient,
        private val chapterCache: ChapterCache) : HttpSourceBase(client, chapterCache) {

    abstract val publisherListSelector: String
    abstract val lastestComicsSelector: String
    abstract val popularComicsSelector: String
    abstract val allComicsListSelector: String
    abstract val searchComicsSelector: String

    override fun parsePublishersResponse(response: Response): List<Publisher> {
        val document = response.asJsoup()

        val publishers: List<Publisher> = document.select(publisherListSelector).map { element ->
            parsePublisherByElement(element)
        }

        return publishers
    }

    abstract fun parsePublisherByElement(element: Element): Publisher

    override fun parseLastestComicsResponse(response: Response): List<ComicsItem> {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(lastestComicsSelector).map { element ->
            parseLastestComicsByElement(element)
        }

        return comics
    }

    abstract fun parseLastestComicsByElement(element: Element): ComicsItem

    override fun parsePopularComicsResponse(response: Response): List<ComicsItem> {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(popularComicsSelector).map { element ->
            parsePopularComicsByElement(element)
        }

        return comics
    }

    abstract fun parsePopularComicsByElement(element: Element): ComicsItem

    override fun parseAllComicsByLetterResponse(response: Response): ComicsListModel {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return ComicsListModel(comics, false)
    }

    abstract fun parseAllComicsByLetterByElement(element: Element): ComicsItem

    override fun parseAllComicsByPublisherResponse(response: Response): ComicsListModel {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return ComicsListModel(comics, false)
    }

    override fun parseAllComicsByScanlatorResponse(response: Response): ComicsListModel {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return ComicsListModel(comics, false)
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): ComicsListModel {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(searchComicsSelector).map { element ->
            parseSearchByQueryByElement(element)
        }

        return ComicsListModel(comics, false)
    }

    abstract fun parseSearchByQueryByElement(element: Element): ComicsItem
}