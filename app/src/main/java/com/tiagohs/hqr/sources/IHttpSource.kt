package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.LocaleDTO
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
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

    fun fetchLastestComics(): Observable<List<ComicViewModel>>
    fun fetchPopularComics(): Observable<List<ComicViewModel>>

    fun fetchReaderComics(hqReaderPath: String, chapterName: String?): Observable<ChapterViewModel>
    fun fetchComicDetails(comicPath: String): Observable<ComicViewModel>

    fun fetchAllComicsByLetter(letter: String): Observable<List<ComicViewModel>>
    fun fetchAllComicsByPublisher(publisherPath: String): Observable<List<ComicViewModel>>
    fun fetchAllComicsByScanlator(scanlatorPath: String): Observable<List<ComicViewModel>>

    fun fetchSearchByQuery(query: String): Observable<List<ComicViewModel>>
    fun fetchPageList(chapter: ChapterViewModel): Observable<List<Page>>

    fun fetchImage(page: Page): Observable<Response>
    fun getCachedImage(page: Page): Observable<Page>
    fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page>
    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page>
}