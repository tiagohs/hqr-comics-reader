package com.tiagohs.hqr.sources

import android.util.Log
import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
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

    override fun parsePublishersResponse(response: Response): List<DefaultModelView> {
        val publishers: ArrayList<DefaultModelView> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(publisherListSelector).map { element ->
                val publisher = parsePublisherByElement(element)

                if (publisher != null)
                    publishers.add(publisher)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return publishers
    }

    abstract fun parsePublisherByElement(element: Element): DefaultModelView?

    override fun parseLastestComicsResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(lastestComicsSelector).map { element ->
                parseLastestComicsByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    abstract fun parseLastestComicsByElement(element: Element): ComicViewModel

    override fun parsePopularComicsResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(popularComicsSelector).map { element ->
                parsePopularComicsByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    abstract fun parsePopularComicsByElement(element: Element): ComicViewModel

    override fun parseAllComicsByLetterResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(allComicsListSelector).map { element ->
                parseAllComicsByLetterByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    abstract fun parseAllComicsByLetterByElement(element: Element): ComicViewModel

    override fun parseAllComicsByPublisherResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(allComicsListSelector).map { element ->
                parseAllComicsByLetterByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    override fun parseAllComicsByScanlatorResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(allComicsListSelector).map { element ->
                parseAllComicsByLetterByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(searchComicsSelector).map { element ->
                parseSearchByQueryByElement(element)
            }

        } catch (ex: Exception) {
            Log.e("ParseError", "Parse Error", ex)
        }

        return comics
    }

    abstract fun parseSearchByQueryByElement(element: Element): ComicViewModel
}