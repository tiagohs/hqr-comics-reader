package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ComicViewModel
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

    override fun parseLastestComicsResponse(response: Response): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(lastestComicsSelector).map { element ->
            parseLastestComicsByElement(element)
        }

        return comics
    }

    abstract fun parseLastestComicsByElement(element: Element): ComicViewModel

    override fun parsePopularComicsResponse(response: Response): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(popularComicsSelector).map { element ->
            parsePopularComicsByElement(element)
        }

        return comics
    }

    abstract fun parsePopularComicsByElement(element: Element): ComicViewModel

    override fun parseAllComicsByLetterResponse(response: Response): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return comics
    }

    abstract fun parseAllComicsByLetterByElement(element: Element): ComicViewModel

    override fun parseAllComicsByPublisherResponse(response: Response): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return comics
    }

    override fun parseAllComicsByScanlatorResponse(response: Response): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(allComicsListSelector).map { element ->
            parseAllComicsByLetterByElement(element)
        }

        return comics
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        val document = response.asJsoup()

        val comics: List<ComicViewModel> = document.select(searchComicsSelector).map { element ->
            parseSearchByQueryByElement(element)
        }

        return comics
    }

    abstract fun parseSearchByQueryByElement(element: Element): ComicViewModel
}