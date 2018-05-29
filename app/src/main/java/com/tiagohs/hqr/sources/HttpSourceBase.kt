package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.ComicsItem
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.service.extensions.asJsoup
import com.tiagohs.hqr.service.extensions.asObservableSuccess
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


abstract class HttpSourceBase(
        private var client: OkHttpClient
) {

    private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
    private val DEFAULT_HEADERS = Headers.Builder().build()
    private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

    private val headers: Headers by lazy { headersBuilder().build() }

    abstract val baseUrl: String

    private fun fetch(request: Request): Observable<Response> {
        return client.newCall(request)
                .asObservableSuccess()
                .subscribeOn(Schedulers.io())
    }

    fun fetchPublishers(): Observable<List<Publisher>> {
        return fetch(GET(getPublishersEndpoint(), headersBuilder().build()))
                    .map({ response: Response -> parsePublishersResponse(response) })
    }

    abstract protected fun getPublishersEndpoint(): String

    abstract protected fun parsePublishersResponse(response: Response) : List<Publisher>

    fun fetchLastestComics(): Observable<List<ComicsItem>> {
        return fetch(GET(getLastestComicsEndpoint(), headersBuilder().build()))
                .map({ response: Response -> parseLastestComicsResponse(response) })
    }

    abstract protected fun getLastestComicsEndpoint(): String

    abstract protected fun parseLastestComicsResponse(response: Response) : List<ComicsItem>

    fun fetchPopularComics(): Observable<List<ComicsItem>> {
        return fetch(GET(getPopularComicsEndpoint(), headersBuilder().build()))
                .map({ response: Response -> parsePopularComicsResponse(response) })
    }

    abstract protected fun getPopularComicsEndpoint(): String

    abstract protected fun parsePopularComicsResponse(response: Response) : List<ComicsItem>

    fun fetchReaderComics(hqReaderPath: String): Observable<Chapter> {
        return fetch(GET(getReaderEndpoint(hqReaderPath), headersBuilder().build()))
                .map({ response: Response -> parsReaderResponse(response) })
    }

    abstract protected fun getReaderEndpoint(hqReaderPath: String): String

    abstract protected fun parsReaderResponse(response: Response) : Chapter

    protected fun GET(url: String,
            headers: Headers = DEFAULT_HEADERS,
            cache: CacheControl = DEFAULT_CACHE_CONTROL): Request {

        return Request.Builder()
                .url(url)
                .headers(headers)
                .cacheControl(cache)
                .build()
    }

    fun fetchResaderComics() {
        fetch(GET("https://www.hqbr.com.br/hqs/Detonador%20(2018)/capitulo/2/leitor/0#1", headersBuilder().build()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    response: Response? ->
                        val document = response!!.asJsoup()
                        val script = document.select("#chapter_pages script").first() // Get the script part

                        val p = Pattern.compile("pages = \\[((.*))\\]") // Regex for the value of the html
                        val m = p.matcher(script.html())

                    while( m.find() )
                    {
                        System.out.println(m.group()); // the whole key ('key = value')
                        System.out.println(m.group(1)); // value only
                    }
                })
    }

    open protected fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
    }
}