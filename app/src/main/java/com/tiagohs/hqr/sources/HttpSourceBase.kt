package com.tiagohs.hqr.sources

import com.tiagohs.hqr.download.cache.ChapterCache
import com.tiagohs.hqr.helpers.extensions.asObservableSuccess
import com.tiagohs.hqr.helpers.extensions.newCallWithProgress
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import java.util.concurrent.TimeUnit


abstract class HttpSourceBase(
        private var client: OkHttpClient,
        private val chapterCache: ChapterCache
): IHttpSource {
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

    override fun fetchPublishers(): Observable<List<DefaultModelView>> {
        return fetch(GET(publishersEndpoint, headersBuilder().build()))
                    .map({ response: Response -> parsePublishersResponse(response) })
    }

    abstract protected fun parsePublishersResponse(response: Response) : List<DefaultModelView>

    override fun fetchLastestComics(): Observable<List<ComicViewModel>> {
        return fetch(GET(lastestComicsEndpoint, headersBuilder().build()))
                .map({ response: Response -> parseLastestComicsResponse(response) })
    }

    abstract protected fun parseLastestComicsResponse(response: Response) : List<ComicViewModel>

    override fun fetchPopularComics(): Observable<List<ComicViewModel>> {
        return fetch(GET(popularComicsEndpoint, headersBuilder().build()))
                .map({ response: Response -> parsePopularComicsResponse(response) })
    }

    abstract protected fun parsePopularComicsResponse(response: Response) : List<ComicViewModel>

    override fun fetchReaderComics(hqReaderPath: String): Observable<ChapterViewModel> {
        return fetch(GET(getReaderEndpoint(hqReaderPath), headersBuilder().build()))
                .map({ response: Response -> parseReaderResponse(response, hqReaderPath) })
    }

    abstract protected fun getReaderEndpoint(hqReaderPath: String): String

    abstract protected fun parseReaderResponse(response: Response, chapterPath: String?) : ChapterViewModel

    override fun fetchComicDetails(comicPath: String): Observable<ComicViewModel> {
        return fetch(GET(getComicDetailsEndpoint(comicPath), headersBuilder().build()))
                .map({ response: Response -> parseComicDetailsResponse(response, comicPath) })
    }

    abstract protected fun getComicDetailsEndpoint(comicPath: String): String

    abstract protected fun parseComicDetailsResponse(response: Response, comicPath: String) : ComicViewModel

    override fun fetchAllComicsByLetter(letter: String): Observable<List<ComicViewModel>> {
        return fetch(GET(getAllComicsByLetterEndpoint(letter), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByLetterResponse(response) })
    }

    abstract protected fun getAllComicsByLetterEndpoint(letter: String): String

    abstract protected fun parseAllComicsByLetterResponse(response: Response) : List<ComicViewModel>

    override fun fetchAllComicsByPublisher(publisherPath: String): Observable<List<ComicViewModel>> {
        return fetch(GET(getAllComicsByPublisherEndpoint(publisherPath), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByPublisherResponse(response) })
    }

    abstract protected fun getAllComicsByPublisherEndpoint(publisherPath: String): String

    abstract protected fun parseAllComicsByPublisherResponse(response: Response) : List<ComicViewModel>

    override fun fetchAllComicsByScanlator(scanlatorPath: String): Observable<List<ComicViewModel>> {
        return fetch(GET(getAllComicsByScanlatorEndpoint(scanlatorPath), headersBuilder().build()))
                .map({ response: Response -> parseAllComicsByScanlatorResponse(response) })
    }

    abstract protected fun getAllComicsByScanlatorEndpoint(scanlatorPath: String): String

    abstract protected fun parseAllComicsByScanlatorResponse(response: Response) : List<ComicViewModel>

    override fun fetchSearchByQuery(query: String): Observable<List<ComicViewModel>> {
        return fetch(GET(getSearchByQueryEndpoint(query), headersBuilder().build()))
                .map({ response: Response -> parseSearchByQueryResponse(response, query) })
    }

    abstract protected fun getSearchByQueryEndpoint(query: String): String

    abstract protected fun parseSearchByQueryResponse(response: Response, query: String): List<ComicViewModel>

    protected fun GET(url: String,
            headers: Headers = DEFAULT_HEADERS,
            cache: CacheControl = DEFAULT_CACHE_CONTROL): Request {

        return Request.Builder()
                .url(url)
                .headers(headers)
                .cacheControl(cache)
                .build()
    }

    override fun fetchPageList(chapter: ChapterViewModel): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
                .asObservableSuccess()
                .map { response -> pageListParse(response, chapter.chapterPath) }
    }

    abstract protected fun pageListParse(response: Response, chapterPath: String?): List<Page>

    open protected fun pageListRequest(chapter: ChapterViewModel): Request {
        return GET(baseUrl + chapter.chapterPath, headers)
    }


    override fun fetchImage(page: Page): Observable<Response> {
        return client.newCallWithProgress(imageRequest(page), page)
                .asObservableSuccess()
    }

    open protected fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun getCachedImage(page: Page): Observable<Page> {
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

    override fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
        return Observable.fromIterable(pages)
                .filter { !it.imageUrl.isNullOrEmpty() }
                .mergeWith(fetchRemainingImageUrlsFromPageList(pages))
    }

    override fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page> {
        return Observable.fromIterable(pages)
                .filter { it.imageUrl.isNullOrEmpty() }
    }

    open protected fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
    }
}