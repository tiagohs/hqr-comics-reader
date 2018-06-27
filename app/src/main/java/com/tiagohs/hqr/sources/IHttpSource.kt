package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.*
import com.tiagohs.hqr.models.viewModels.ComicsListModel
import io.reactivex.Observable
import okhttp3.Response

interface IHttpSource {
    val id: Long
    val name: String
    val baseUrl: String
    val language: LocaleDTO

    val hasPageSupport: Boolean
    val hasThumbnailSupport: Boolean

    fun fetchPublishers(): Observable<List<Publisher>>

    fun fetchLastestComics(): Observable<List<ComicsItem>>
    fun fetchPopularComics(): Observable<List<ComicsItem>>

    fun fetchReaderComics(hqReaderPath: String, chapterName: String?, comicId: String?): Observable<Chapter>
    fun fetchComicDetails(comicPath: String): Observable<Comic>

    fun fetchAllComicsByLetter(letter: String): Observable<ComicsListModel>
    fun fetchAllComicsByPublisher(publisherPath: String): Observable<ComicsListModel>
    fun fetchAllComicsByScanlator(scanlatorPath: String): Observable<ComicsListModel>

    fun fetchSearchByQuery(query: String): Observable<ComicsListModel>
    fun fetchPageList(chapter: Chapter): Observable<List<Page>>

    fun fetchImage(page: Page): Observable<Response>
    fun getCachedImage(page: Page): Observable<Page>
    fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page>
    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page>
}