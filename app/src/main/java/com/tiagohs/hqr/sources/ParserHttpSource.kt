package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asJsoup
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import timber.log.Timber

abstract class ParserHttpSource(
        private var client: OkHttpClient,
        private val chapterCache: ChapterCache) : HttpSourceBase(client, chapterCache) {

    abstract val homeCategoriesListSelector: String
    abstract val lastestComicsSelector: String
    abstract val popularComicsSelector: String
    abstract val allComicsListSelector: String
    abstract val allComicsByPublisherSelector: String
    abstract val searchComicsSelector: String


    override fun parsePublishersResponse(response: Response): List<DefaultModelView> {
        val publishers: ArrayList<DefaultModelView> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(homeCategoriesListSelector).map { element ->
                val publisher = parseHomeCategoriesByElement(element)

                if (publisher != null)
                    publishers.add(publisher)
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return publishers
    }

    abstract fun parseHomeCategoriesByElement(element: Element): DefaultModelView?

    override fun parseLastestComicsResponse(response: Response): List<ComicViewModel> {
        var comics: List<ComicViewModel> = emptyList()

        try {
            val document = response.asJsoup()

            comics = document.select(lastestComicsSelector).map { element ->
                parseLastestComicsByElement(element)
            }

        } catch (ex: Exception) {
            Timber.e(ex)
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
            Timber.e(ex)
        }

        return comics
    }

    abstract fun parsePopularComicsByElement(element: Element): ComicViewModel

    override fun parseAllComicsByLetterResponse(response: Response): List<ComicViewModel> {
        val comics: ArrayList<ComicViewModel> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(allComicsListSelector).forEach { element ->
                val comic = parseAllComicsByLetterByElement(element)

                if (comic != null) {
                    comics.add(comic)
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return comics
    }

    abstract fun parseAllComicsByLetterByElement(element: Element): ComicViewModel?

    override fun parseAllComicsByPublisherResponse(response: Response): List<ComicViewModel> {
        val comics: ArrayList<ComicViewModel> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(allComicsByPublisherSelector).forEach { element ->
                val comic = parseAllComicsByPublisherByElement(element)

                if (comic != null) {
                    comics.add(comic)
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return comics
    }

    abstract fun parseAllComicsByPublisherByElement(element: Element): ComicViewModel?

    override fun parseAllComicsByScanlatorResponse(response: Response): List<ComicViewModel> {
        val comics: ArrayList<ComicViewModel> = ArrayList()

        try {
            val document = response.asJsoup()

             document.select(allComicsListSelector).forEach { element ->
                val comic = parseAllComicsByLetterByElement(element)

                if (comic != null) {
                    comics.add(comic)
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return comics
    }

    override fun parseAllComicsByGenreResponse(response: Response): List<ComicViewModel> {
        val comics: ArrayList<ComicViewModel> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(allComicsListSelector).forEach { element ->
                val comic = parseAllComicsByLetterByElement(element)

                if (comic != null) {
                    comics.add(comic)
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return comics
    }

    abstract fun parseAllComicsByGenreByElement(element: Element): ComicViewModel?

    override fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel> {
        val comics: ArrayList<ComicViewModel> = ArrayList()

        try {
            val document = response.asJsoup()

            document.select(searchComicsSelector).map { element ->
                val comic = parseAllComicsByLetterByElement(element)

                if (comic != null) {
                    comics.add(comic)
                }
            }

        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return comics
    }

    abstract fun parseSearchByQueryByElement(element: Element): ComicViewModel?
}