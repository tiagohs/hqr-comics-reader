package com.tiagohs.hqr.sources

import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.LocaleDTO
import com.tiagohs.hqr.models.sources.Page
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.models.viewModels.ChapterViewModel
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import com.tiagohs.hqr.models.viewModels.ComicsListViewModel
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

    fun fetchAllComicsByLetter(letter: String): Observable<ComicsListViewModel>
    fun fetchAllComicsByPublisher(publisherPath: String): Observable<ComicsListViewModel>
    fun fetchAllComicsByScanlator(scanlatorPath: String): Observable<ComicsListViewModel>

    fun fetchSearchByQuery(query: String): Observable<ComicsListViewModel>
    fun fetchPageList(chapter: Chapter): Observable<List<Page>>

    fun fetchImage(page: Page): Observable<Response>
    fun getCachedImage(page: Page): Observable<Page>
    fun fetchAllImageUrlsFromPageList(pages: List<Page>): Observable<Page>
    fun fetchRemainingImageUrlsFromPageList(pages: List<Page>): Observable<Page>
}