package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.service.extensions.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element

abstract class ParserHttpSource(
        private var client: OkHttpClient) : HttpSourceBase(client) {

    override fun parsePublishersResponse(response: Response): List<Publisher> {
        val document = response.asJsoup()

        val publishers: List<Publisher> = document.select(getPublisherListSelector()).map { element ->
            parsePublisherByElement(element)
        }

        return publishers
    }

    abstract fun getPublisherListSelector(): String

    abstract fun parsePublisherByElement(element: Element): Publisher

    override fun parseLastestComicsResponse(response: Response): List<ComicsItem> {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(getLastestComicsSelector()).map { element ->
            parseLastestComicsByElement(element)
        }

        return comics
    }

    abstract fun getLastestComicsSelector(): String

    abstract fun parseLastestComicsByElement(element: Element): ComicsItem

    override fun parsePopularComicsResponse(response: Response): List<ComicsItem> {
        val document = response.asJsoup()

        val comics: List<ComicsItem> = document.select(getPopularComicsSelector()).map { element ->
            parsePopularComicsByElement(element)
        }

        return comics
    }

    abstract fun getPopularComicsSelector(): String

    abstract fun parsePopularComicsByElement(element: Element): ComicsItem
}