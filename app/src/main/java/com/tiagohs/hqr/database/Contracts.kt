package com.tiagohs.hqr.database

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.database.comics.ComicHistory
import io.reactivex.Observable

interface ISourceRepository {

    fun insertSource(sourceDB: SourceDB): Observable<SourceDB>
    fun getAllSources(): Observable<List<SourceDB>>
    fun getAllCatalogueSources(): Observable<List<CatalogueSource>>

    fun getCatalogueSourceById(catalogueSourceId: Long): Observable<CatalogueSource>
    fun getSourceById(sourceId: Long): Observable<SourceDB>
}

interface IComicsRepository {

    fun insertComic(comic: Comic): Observable<Comic>
    fun insertComics(comics: List<Comic>): Observable<List<Comic>>
    fun deleteComic(comic: Comic): Observable<Void>

    fun deleteAllComics(): Observable<Void?>
    fun getAllComics(): Observable<List<Comic>>

    fun getComic(comicId: Long): Observable<Comic>
    fun getComic(pathLink: String, sourceId: Long):  Observable<Comic>

    fun getPopularComics(): Observable<List<Comic>>
    fun getRecentsComics(): Observable<List<Comic>>
    fun getFavoritesComics(): Observable<List<Comic>>

    fun getTotalChapters(comic: Comic): Observable<Int>
}

interface IChapterRepository {

    fun insertChapter(chapter: Chapter): Observable<Chapter>
    fun insertChapters(chapters: List<Chapter>): Observable<List<Chapter>>
    fun deleteChapter(chapter: Chapter): Observable<Void>
    fun deleteChapters(chapters: List<Chapter>): Observable<Void>

    fun deleteAllChapters(): Observable<Void?>
    fun getAllChapters(comic: Comic): Observable<List<Chapter>>

    fun getChapter(chapterId: Long): Observable<Chapter>
    fun getChapter(pathLink: String, comicId: Long): Observable<Chapter>
}

interface IHistoryRepository {

    fun insertComicHistory(comicHistory: ComicHistory): Observable<ComicHistory>
    fun deleteComicHistory(comicHistory: ComicHistory): Observable<Void>

    fun delteAllComicHistory(): Observable<Void?>
    fun getAllComicHistory(): Observable<List<ComicHistory>>

    fun getComicHistory(id: Long): Observable<ComicHistory>
}

