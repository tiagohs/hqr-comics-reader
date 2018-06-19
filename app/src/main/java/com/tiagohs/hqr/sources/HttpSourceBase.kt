package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asObservableSuccess
import com.tiagohs.hqr.helpers.extensions.newCallWithProgress
import com.tiagohs.hqr.models.sources.*
import com.tiagohs.hqr.models.viewModels.ComicsListModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import java.util.concurrent.TimeUnit


abstract class HttpSourceBase(
        private var client: OkHttpClient,
        private val chapterCache: ChapterCache
): ISource {

    private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
    private val DEFAULT_HEADERS = Headers.Builder().build()
    private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

    private val headers: Headers by lazy { headersBuilder().build() }

    abstract protected val publishersEndpoint: String
    abstract protected val lastestComicsEndpoint: String
    abstract protected val popularComicsEndpoint: String

    private fun fetch(request: Request): Observable<Response> {
        return client.newCall(request)
                .asObservableSuccess()
                .subscribeOn(Schedulers.io())
    }

    fun fetchPublishers(): Observable<List<Publisher>> {
        return fetch(GET(publishersEndpoint, headersBuilder().build()))
                    .map({ response: Response -> parsePublishersResponse(response) })
    }

    abstract protected fun parsePublishersResponse(response: Response) : List<Publisher>

    fun fetchLastestComics(): Observable<List<ComicsItem>> {
        return fetch(GET(lastestComicsEndpoint, headersBuilder().build()))
                .map({ response: Response -> parseLastestComicsResponse(response) })
    }

    abstract protected fun parseLastestComicsResponse(response: Response) : List<ComicsItem>

    fun fetchPopularComics(): Observable<List<ComicsItem>> {
        return fetch(GET(popularComicsEndpoint, headersBuilder().build()))
                .map({ response: Response -> parsePopularComicsResponse(response) })
    }

    abstract protected fun parsePopularComicsResponse(response: Response) : List<ComicsItem>

    fun fetchReaderComics(hqReaderPath: String, chapterName: String?, comicId: String?): Observable<Chapter> {
        return fetch(GET(getReaderEndpoint(hqReaderPath), headersBuilder().build()))
                .map({ response: Response -> parseReaderResponse(response, chapterName, hqReaderPath, comicId) })
    }

    abstract protected fun getReaderEndpoint(hqReaderPath: String): String

    abstract protected fun parseReaderResponse(response: Response, chapterName: String?, chapterPath: String?, comicId: String?) : Chapter

    fun fetchComicDetails(comicPath: String): Observable<Comic> {
        return fetch(GET(getComicDetailsEndpoint(comicPath), headersBuilder().build()))
                .map({ response: Response -> parseComicDetailsResponse(response, comicPath) })
    }

    abstract protected fun getComicDetailsEndpoint(comicPath: String): String

    abstract protected fun parseComicDetailsResponse(response: Response, comicPath: String) : Comic

    fun fetchAllComicsByLetter(letter: String): Observable<ComicsListModel> {
        return fetch(GET(getAllComicsByLetterEndpoint(letter), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByLetterResponse(response) })
    }

    abstract protected fun getAllComicsByLetterEndpoint(letter: String): String

    abstract protected fun parseAllComicsByLetterResponse(response: Response) : ComicsListModel

    fun fetchAllComicsByPublisher(publisherPath: String): Observable<ComicsListModel> {
        return fetch(GET(getAllComicsByPublisherEndpoint(publisherPath), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByPublisherResponse(response) })
    }

    abstract protected fun getAllComicsByPublisherEndpoint(publisherPath: String): String

    abstract protected fun parseAllComicsByPublisherResponse(response: Response) : ComicsListModel

    fun fetchAllComicsByScanlator(scanlatorPath: String): Observable<ComicsListModel> {
        return fetch(GET(getAllComicsByScanlatorEndpoint(scanlatorPath), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByScanlatorResponse(response) })
    }

    abstract protected fun getAllComicsByScanlatorEndpoint(scanlatorPath: String): String

    abstract protected fun parseAllComicsByScanlatorResponse(response: Response) : ComicsListModel

    fun fetchSearchByQuery(query: String): Observable<ComicsListModel> {
        return fetch(GET(getSearchByQueryEndpoint(query), headersBuilder().build()))
                .map({ response: Response -> parseSearchByQueryResponse(response, query) })
    }

    abstract protected fun getSearchByQueryEndpoint(query: String): String

    abstract protected fun parseSearchByQueryResponse(response: Response, query: String): ComicsListModel

    protected fun GET(url: String,
            headers: Headers = DEFAULT_HEADERS,
            cache: CacheControl = DEFAULT_CACHE_CONTROL): Request {

        return Request.Builder()
                .url(url)
                .headers(headers)
                .cacheControl(cache)
                .build()
    }

    fun fetchPageList(chapter: Chapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
                .asObservableSuccess()
                .map { response -> pageListParse(response, chapter.chapterPath) }
    }

    abstract protected fun pageListParse(response: Response, chapterPath: String?): List<Page>

    open protected fun pageListRequest(chapter: Chapter): Request {
        return GET(baseUrl + chapter.chapterPath, headers)
    }


    fun fetchImage(page: Page): Observable<Response> {
        return client.newCallWithProgress(imageRequest(page), page)
                .asObservableSuccess()
    }

    open protected fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    fun getCachedImage(page: Page): Observable<Page> {
        val imageUrl = page.imageUrl ?: return Observable.just(page)

        return Observable.just(page)
                .flatMap {
                    if (!chapterCache.isImageInCache(imageUrl)) {
                        cacheImage(page)
                    } else {
                        Observable.just(page)
                    }
                }
                .doOnNext {
                    page.uri = android.net.Uri.fromFile(chapterCache.getImageFile(imageUrl))
                    page.status = Page.READY
                }
                .doOnError { page.status = Page.ERROR }
                .onErrorReturn { page }
    }

    private fun cacheImage(page: Page): Observable<Page> {
        page.status = Page.DOWNLOAD_IMAGE
        return fetchImage(page)
                .doOnNext { chapterCache.putImageToCache(page.imageUrl!!, it) }
                .map { page }
    }

    fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
        return Observable.fromIterable(pages)
                .filter { !it.imageUrl.isNullOrEmpty() }
                .mergeWith(fetchRemainingImageUrlsFromPageList(pages))
    }

    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
        return Observable.fromIterable(pages)
                .filter { it.imageUrl.isNullOrEmpty() }
    }

    open protected fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
    }
}