package com.tiagohs.hqr.database

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.database.comics.ComicHistory
import com.tiagohs.hqr.models.viewModels.ComicViewModel
import io.reactivex.Observable
import com.tiagohs.hqr.models.sources.Chapter as NetworkChapter
import com.tiagohs.hqr.models.sources.Comic as NetworkComic
import com.tiagohs.hqr.models.sources.ComicsItem as NetworkComicsItem

interface ISourceRepository {

    fun getSourceByIdRealm(sourceId: Long): SourceDB?

    fun insertSource(sourceDB: SourceDB): Observable<SourceDB>
    fun getAllSources(): Observable<List<SourceDB>?>
    fun getAllCatalogueSources(): Observable<List<CatalogueSource>?>

    fun getCatalogueSourceById(catalogueSourceId: Long): Observable<CatalogueSource?>
    fun getSourceById(sourceId: Long): Observable<SourceDB?>
}

interface IComicsRepository {

    fun insertRealm(comic: ComicViewModel, sourceId: Long): ComicViewModel?
    fun insertRealm(comics: List<ComicViewModel>, sourceId: Long): List<ComicViewModel>?
    fun findByPathUrlRealm(pathLink: String, sourceId: Long): ComicViewModel?

    fun insertOrUpdateComic(comic: ComicViewModel, sourceId: Long): Observable<ComicViewModel>
    fun insertOrUpdateComics(comics: List<ComicViewModel>, sourceId: Long): Observable<List<ComicViewModel>>

    fun deleteComic(comic: ComicViewModel): Observable<Void>

    fun deleteAllComics(): Observable<Void?>
    fun findAll(sourceId: Long): Observable<List<ComicViewModel>>

    fun findById(comicId: Long): Observable<ComicViewModel?>
    fun findByPathUrl(pathLink: String, sourceId: Long):  Observable<ComicViewModel?>

    fun getPopularComics(sourceId: Long): Observable<List<ComicViewModel>>
    fun getRecentsComics(sourceId: Long): Observable<List<ComicViewModel>>
    fun getFavoritesComics(): Observable<List<ComicViewModel>>

    fun getTotalChapters(comic: ComicViewModel): Observable<Int>

    fun checkIfIsSaved(comics: List<ComicViewModel>): Observable<List<ComicViewModel>>
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

